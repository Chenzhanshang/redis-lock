package org.springframework.boot.autoconfigure.klock.lock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.klock.model.LockInfo;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by kl on 2017/12/29.
 */
public class ReentrantLock implements Lock {

    private static final Logger logger = LoggerFactory.getLogger(ReentrantLock.class);


    private  RLock rLock;

    private final LockInfo lockInfo;

    private RedissonClient redissonClient;

    public ReentrantLock(RedissonClient redissonClient,LockInfo lockInfo) {
        this.redissonClient = redissonClient;
        this.lockInfo = lockInfo;
    }
    @Override
    public boolean acquire() {
        try {
            rLock = redissonClient.getLock(lockInfo.getLockName());
            return rLock.tryLock(lockInfo.getWaitTime(), lockInfo.getLeaseTime(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    @Override
    public boolean release() {
        if(rLock.isHeldByCurrentThread()){
            try {
                return rLock.forceUnlockAsync().get();
            } catch (InterruptedException e) {
                return false;
            } catch (ExecutionException e) {
                return false;
            }
        }
        return false;
    }

    public String getKey(){
        return this.lockInfo.getLockName();
    }
}
