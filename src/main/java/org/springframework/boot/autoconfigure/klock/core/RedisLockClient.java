package org.springframework.boot.autoconfigure.klock.core;

import com.alibaba.fastjson.JSON;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.klock.lock.Lock;
import org.springframework.boot.autoconfigure.klock.lock.LockFactory;
import org.springframework.boot.autoconfigure.klock.model.LockInfo;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Redis分布式锁客户端
 *
 * @author Jorvey
 * @date 2020年04月07日
 */
@Slf4j
public class RedisLockClient {

    @Autowired
    LockFactory lockFactory;

    private final Map<String, LockRes> currentThreadLock = new ConcurrentHashMap<>();

    public <T> T lock(LockHandler<T> handler, LockInfo<T> lockInfo) throws Throwable {

        T handle = null;
        String currentLock = this.getCurrentLockId(lockInfo);

        log.trace("-currentLock-：" + currentLock);

        currentThreadLock.put(currentLock, new LockRes(lockInfo, false));

        try {
            Lock lock = lockFactory.getLock(lockInfo);
            boolean lockRes = lock.acquire();

            //如果获取锁失败了，则进入失败的处理逻辑
            if (!lockRes) {
                log.trace("Timeout while acquiring Lock({})", lockInfo.getLockName());
                //如果自定义了获取锁失败的处理策略，则执行自定义的降级处理策略
                if (!StringUtils.isEmpty(lockInfo.getCustomLockTimeoutHandler())) {
                    return lockInfo.getCustomLockTimeoutHandler().handle();

                } else {
                    //否则执行预定义的执行策略
                    //注意：如果没有指定预定义的策略，默认的策略为静默啥不做处理
                    lockInfo.getLockTimeoutStrategy().handle(lockInfo, lock);
                }
            }

            //获得锁 设置当前线程锁
            currentThreadLock.get(currentLock).setLock(lock);
            //获得锁 设置当前线程结果
            currentThreadLock.get(currentLock).setRes(true);

            log.trace("handler before");
            T result = handler.handle();
            log.trace("handler after");
            return result;
        } finally {
            log.trace("finally");
            releaseLock(lockInfo, currentLock);
            cleanUpThreadLocal(currentLock);
        }
    }

    /**
     * 释放锁
     */
    private void releaseLock(LockInfo lockInfo, String currentLock) throws Throwable {
        log.trace("releaseLock:" + currentLock + "===>" + lockInfo);
        LockRes lockRes = currentThreadLock.get(currentLock);
        log.trace("lockRes:" + JSON.toJSONString(lockRes));

        if (Objects.isNull(lockRes)) {
            throw new NullPointerException("Please check whether the input parameter used as the lock key value " +
                    "has been modified in the method, " +
                    "which will cause the acquire and release locks to have different key values " +
                    "and throw null pointers.currentLockKey:" + currentLock);
        }

        if (lockRes.getRes()) {
            //成功获得锁的线程，需要释放锁
            boolean releaseRes = currentThreadLock.get(currentLock).getLock().release();
            // avoid release lock twice when exception happens below
            lockRes.setRes(false);
            if (!releaseRes) {
                log.trace("releaseLock=>releaseRes:" + releaseRes);
                //释放锁失败
                handleReleaseTimeout(lockRes.getLockInfo());
            }
        }
    }

    // avoid memory leak
    private void cleanUpThreadLocal(String currentLock) {
        log.trace("cleanUpThreadLocal=>" + currentLock);
        currentThreadLock.remove(currentLock);
    }

    /**
     * 获取当前锁在map中的key
     *
     * @param lockInfo
     * @return
     */
    public String getCurrentLockId(LockInfo lockInfo) {
        return Thread.currentThread().getId() + lockInfo.getLockName();
    }

    /**
     * 处理释放锁时已超时
     */
    private void handleReleaseTimeout(LockInfo lockInfo) throws Throwable {

        log.trace("Timeout while release Lock({})", lockInfo.getLockName());

        if (!ObjectUtils.isEmpty(lockInfo.getCustomReleaseTimeoutHandler())) {

            lockInfo.getCustomReleaseTimeoutHandler().handle();

        } else {
            lockInfo.getReleaseTimeoutStrategy().handle(lockInfo);
//        }

        }
    }

    /**
     * 处理自定义释放锁时已超时
     */
    private void handleCustomReleaseTimeout(LockHandler releaseTimeoutHandler) throws Throwable {
        releaseTimeoutHandler.handle();
    }


    @Getter
    @Setter
    @NoArgsConstructor
    private class LockRes {

        private LockInfo lockInfo;
        private Lock lock;
        private Boolean res;

        LockRes(LockInfo lockInfo, Boolean res) {
            this.lockInfo = lockInfo;
            this.res = res;
        }
    }
}

