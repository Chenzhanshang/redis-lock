package org.springframework.boot.autoconfigure.klock.core;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.klock.annotation.Klock;
import org.springframework.boot.autoconfigure.klock.handler.KlockInvocationException;
import org.springframework.boot.autoconfigure.klock.lock.Lock;
import org.springframework.boot.autoconfigure.klock.lock.LockFactory;
import org.springframework.boot.autoconfigure.klock.model.LockInfo;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by kl on 2017/12/29.
 * Content :给添加@KLock切面加锁处理
 */
@Aspect
@Component
@Order(0)
@Slf4j
public class KlockAspectHandler {

    @Autowired
    LockFactory lockFactory;

    @Autowired
    private LockInfoProvider lockInfoProvider;

    @Autowired
    private RedisLockClient redisLockClient;


    @Around(value = "@annotation(klock)")
    public Object around(ProceedingJoinPoint joinPoint, Klock klock) throws Throwable {
        log.trace("start=====around=======");
        long start = System.nanoTime();

        LockInfo lockInfo = lockInfoProvider.get(joinPoint, klock);
        Object result = redisLockClient.lock(joinPoint::proceed, lockInfo);

        long end = System.nanoTime();
        log.trace("distributed lockable cost:" + (end - start) + "ns");
        log.trace("end======around======");
        return result;
    }
}
