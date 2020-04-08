package org.springframework.boot.autoconfigure.klock.lock;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jorvey
 */
abstract public class DistributedLock implements Lock, AutoCloseable {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    abstract public boolean release();

    @Override
    public void close() throws Exception {

        logger.debug("distributed lock close , {}", this.toString());

        this.release();
    }
}