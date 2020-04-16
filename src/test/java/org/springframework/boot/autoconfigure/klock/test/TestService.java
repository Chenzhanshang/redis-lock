package org.springframework.boot.autoconfigure.klock.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.klock.annotation.Klock;
import org.springframework.boot.autoconfigure.klock.annotation.KlockKey;
import org.springframework.boot.autoconfigure.klock.core.RedisLockClient;
import org.springframework.boot.autoconfigure.klock.model.LockInfo;
import org.springframework.boot.autoconfigure.klock.model.LockTimeoutStrategy;
import org.springframework.boot.autoconfigure.klock.model.LockType;
import org.springframework.stereotype.Service;

/**
 * Created by kl on 2017/12/29.
 */
@Service
public class TestService {

    @Autowired
    private RedisLockClient redisLockClient;

    @Klock(keys = {"#param"},
        lockTimeoutStrategy = LockTimeoutStrategy.FAIL_FAST,
        customLockTimeoutStrategy = "lockParams",customReleaseTimeoutStrategy = "releaseParams")
    public String getValue(String param) throws Exception {

        System.out.println(param + "================" + Thread.currentThread().getName());
        //  if ("sleep".equals(param)) {//线程休眠或者断点阻塞，达到一直占用锁的测试效果
        Thread.sleep(1000 * 3);
        //}
        return "success";
    }


    public String getParams(String param) throws Throwable {

        System.out.println(param + "================" + Thread.currentThread().getName());

        LockInfo lockInfo = new LockInfo(LockType.Reentrant, param,
                1, () -> lockParams(param),
                6,() -> releaseParams(param));

        LockInfo lockInfo1 = new LockInfo(LockType.Reentrant, param,
                1,6);


        return redisLockClient.lock(() -> {
            System.out.println("执行业务前");
            Thread.sleep(1000 * 3);
            System.out.println("执行业务后");
            return "success";
        }, lockInfo1);
    }

    private String lockParams(String param) {
        System.out.println("lockParams");
        return "lockParams";
    }

    private String releaseParams(String param) {
        System.out.println("releaseParams");
        return "releaseParams";
    }

    @Klock(keys = {"#userId"})
    public String getValue(String userId, @KlockKey Integer id) throws Exception {
        Thread.sleep(60 * 1000);
        return "success";
    }

    @Klock(keys = {"#user.name", "#user.id"})
    public String getValue(User user) throws Exception {
        Thread.sleep(60 * 1000);
        return "success";
    }

}
