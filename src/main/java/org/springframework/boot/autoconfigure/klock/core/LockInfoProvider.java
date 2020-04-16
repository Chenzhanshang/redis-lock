package org.springframework.boot.autoconfigure.klock.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.klock.annotation.Klock;
import org.springframework.boot.autoconfigure.klock.config.KlockConfig;
import org.springframework.boot.autoconfigure.klock.handler.KlockInvocationException;
import org.springframework.boot.autoconfigure.klock.model.LockInfo;
import org.springframework.boot.autoconfigure.klock.model.LockType;
import org.springframework.util.StringUtils;

/**
 * Created by kl on 2017/12/29.
 */
@Slf4j
public class LockInfoProvider {

  private static final String LOCK_NAME_PREFIX = "lock";
  private static final String LOCK_NAME_SEPARATOR = ".";


  @Autowired
  private KlockConfig klockConfig;

  @Autowired
  private BusinessKeyProvider businessKeyProvider;

  LockInfo get(JoinPoint joinPoint, Klock klock) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    LockType type = klock.lockType();
    String businessKeyName = businessKeyProvider.getKeyName(joinPoint, klock);
    //锁的名字，锁的粒度就是这里控制的
    String lockName = LOCK_NAME_PREFIX + LOCK_NAME_SEPARATOR + getLockName(klock.name(), signature)
        + businessKeyName;
    long waitTime = getWaitTime(klock);
    long leaseTime = getLeaseTime(klock);

    String customLockTimeoutStrategy = klock.customLockTimeoutStrategy();

    LockHandler customLockTimeoutHandler = null;
    if (StringUtils.hasLength(customLockTimeoutStrategy)) {
      customLockTimeoutHandler = () -> handleCustomLockTimeout(customLockTimeoutStrategy,
          joinPoint);
    }

    String customReleaseTimeoutStrategy = klock.customReleaseTimeoutStrategy();
    LockHandler customReleaseTimeoutHandler = null;
    if (StringUtils.hasLength(customReleaseTimeoutStrategy)) {
      customReleaseTimeoutHandler = () -> handleCustomReleaseTimeout(customReleaseTimeoutStrategy,
          joinPoint);
    }

    return getLockInfo(type, lockName, waitTime, customLockTimeoutHandler,
        leaseTime, customReleaseTimeoutHandler);
  }

  /**
   * 处理自定义加锁超时
   */
  private Object handleCustomLockTimeout(String lockTimeoutHandler, JoinPoint joinPoint)
      throws Throwable {

    // prepare invocation context
    Method currentMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();
    Object target = joinPoint.getTarget();
    Method handleMethod = null;
    try {
      handleMethod = joinPoint.getTarget().getClass()
          .getDeclaredMethod(lockTimeoutHandler, currentMethod.getParameterTypes());
      handleMethod.setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException("Illegal annotation param customLockTimeoutStrategy", e);
    }
    Object[] args = joinPoint.getArgs();

    // invoke
    Object res = null;
    try {
      res = handleMethod.invoke(target, args);
    } catch (IllegalAccessException e) {
      throw new KlockInvocationException(
          "Fail to invoke custom lock timeout handler: " + lockTimeoutHandler, e);
    } catch (InvocationTargetException e) {
      throw e.getTargetException();
    }

    return res;
  }

  /**
   * 处理自定义释放锁时已超时
   */
  private Object handleCustomReleaseTimeout(String releaseTimeoutHandler, JoinPoint joinPoint)
      throws Throwable {

    Method currentMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();
    Object target = joinPoint.getTarget();
    Method handleMethod = null;
    try {
      handleMethod = joinPoint.getTarget().getClass()
          .getDeclaredMethod(releaseTimeoutHandler, currentMethod.getParameterTypes());
      handleMethod.setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException("Illegal annotation param customReleaseTimeoutStrategy",
          e);
    }
    Object[] args = joinPoint.getArgs();

    try {
      handleMethod.invoke(target, args);
    } catch (IllegalAccessException e) {
      throw new KlockInvocationException(
          "Fail to invoke custom release timeout handler: " + releaseTimeoutHandler, e);
    } catch (InvocationTargetException e) {
      throw e.getTargetException();
    }
    return args;
  }


  public LockInfo getLockInfo(LockType type, String lockName, long waitTime,
      LockHandler customLockTimeoutHandler
      , long leaseTime, LockHandler customReleaseTimeoutHandler) {
    //如果占用锁的时间设计不合理，则打印相应的警告提示
    if (leaseTime == -1 && log.isWarnEnabled()) {
      log.warn("Trying to acquire Lock({}) with no expiration, " +
          "Klock will keep prolong the lock expiration while the lock is still holding by current thread. "
          +
          "This may cause dead lock in some circumstances.", lockName);
    }
    return new LockInfo(type, lockName, waitTime, customLockTimeoutHandler, leaseTime,
        customReleaseTimeoutHandler);
  }

  /**
   * 获取锁的name，如果没有指定，则按全类名拼接方法名处理
   *
   * @param annotationName
   * @param signature
   * @return
   */
  private String getLockName(String annotationName, MethodSignature signature) {
    if (annotationName.isEmpty()) {
      return String
          .format("%s.%s", signature.getDeclaringTypeName(), signature.getMethod().getName());
    } else {
      return annotationName;
    }
  }


  private long getWaitTime(Klock lock) {
    return lock.waitTime() == Long.MIN_VALUE ?
        klockConfig.getWaitTime() : lock.waitTime();
  }

  private long getLeaseTime(Klock lock) {
    return lock.leaseTime() == Long.MIN_VALUE ?
        klockConfig.getLeaseTime() : lock.leaseTime();
  }
}
