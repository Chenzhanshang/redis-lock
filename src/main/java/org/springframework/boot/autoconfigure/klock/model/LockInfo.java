package org.springframework.boot.autoconfigure.klock.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.klock.config.KlockConfig;
import org.springframework.boot.autoconfigure.klock.core.LockHandler;

/**
 * Created by kl on 2017/12/29. Content :锁基本信息
 */
@Getter
@Setter
public class LockInfo<T> {

  private String lockName;
  private LockType type = LockType.Reentrant;
  private long waitTime = KlockConfig.DEFAULT_WAIT_TIME;
  private long leaseTime = KlockConfig.DEFAULT_LEASE_TIME;
  /**
   * 加锁超时的处理策略
   *
   * @return lockTimeoutStrategy
   */
  private LockTimeoutStrategy lockTimeoutStrategy = LockTimeoutStrategy.FAIL_FAST;

  /**
   * 释放锁时已超时的处理策略
   *
   * @return lockTimeoutHandler
   */
  private ReleaseTimeoutStrategy releaseTimeoutStrategy = ReleaseTimeoutStrategy.FAIL_FAST;
  ;

  /**
   * 自定义释放锁时已超时的处理策略
   *
   * @return customLockTimeoutHandler
   */
  private LockHandler<T> customLockTimeoutHandler;

  /**
   * 自定义释放锁时已超时的处理策略
   *
   * @return releaseTimeoutHandler
   */
  private LockHandler<T> customReleaseTimeoutHandler;

  public LockInfo(LockType type, String lockName,
      long waitTime, LockTimeoutStrategy lockTimeoutStrategy,
      long leaseTime, ReleaseTimeoutStrategy releaseTimeoutStrategy,
      LockHandler<T> lockTimeoutHandler, LockHandler<T> releaseTimeoutHandler
  ) {
    if (type != null) {
      this.type = type;
    }
    if (lockName != null) {
      this.lockName = lockName;
    }
    if (waitTime != Long.MIN_VALUE) {
      this.waitTime = waitTime;
    }
    if (leaseTime != Long.MIN_VALUE) {
      this.leaseTime = leaseTime;
    }
    if (lockTimeoutStrategy != null) {
      this.lockTimeoutStrategy = lockTimeoutStrategy;
    }
    if (releaseTimeoutStrategy != null) {
      this.releaseTimeoutStrategy = releaseTimeoutStrategy;
    }
    if (lockTimeoutHandler != null) {
      this.customLockTimeoutHandler = lockTimeoutHandler;
    }
    if (releaseTimeoutHandler != null) {
      this.customReleaseTimeoutHandler = releaseTimeoutHandler;
    }
  }

  public LockInfo(LockType type, String lockName, long waitTime, long leaseTime) {
    this(type, lockName, waitTime, null, leaseTime, null, null, null);
  }

  public LockInfo(LockType type, String lockName,
      long waitTime, LockHandler<T> lockTimeoutHandler,
      long leaseTime, LockHandler<T> releaseTimeoutHandler) {
    this(type, lockName, waitTime, null, leaseTime, null, lockTimeoutHandler,
        releaseTimeoutHandler);
  }

  public LockInfo(String lockName) {
    this.lockName = lockName;
  }

}
