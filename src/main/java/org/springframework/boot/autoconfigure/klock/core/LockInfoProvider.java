package org.springframework.boot.autoconfigure.klock.core;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.klock.annotation.Klock;
import org.springframework.boot.autoconfigure.klock.config.KlockConfig;
import org.springframework.boot.autoconfigure.klock.model.LockInfo;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.boot.autoconfigure.klock.model.LockType;

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
        String lockName = LOCK_NAME_PREFIX + LOCK_NAME_SEPARATOR + getLockName(klock.name(), signature) + businessKeyName;
        long waitTime = getWaitTime(klock);
        long leaseTime = getLeaseTime(klock);
        return get(type, lockName, waitTime, leaseTime);
    }

    LockInfo get(LockType type, String lockName, long waitTime, long leaseTime) {
        //如果占用锁的时间设计不合理，则打印相应的警告提示
        if (leaseTime == -1 && log.isWarnEnabled()) {
            log.warn("Trying to acquire Lock({}) with no expiration, " +
                    "Klock will keep prolong the lock expiration while the lock is still holding by current thread. " +
                    "This may cause dead lock in some circumstances.", lockName);
        }
        return new LockInfo(type, lockName, waitTime, leaseTime);
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
            return String.format("%s.%s", signature.getDeclaringTypeName(), signature.getMethod().getName());
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
