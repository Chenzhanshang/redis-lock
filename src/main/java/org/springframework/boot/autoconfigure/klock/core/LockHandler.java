package org.springframework.boot.autoconfigure.klock.core;

@FunctionalInterface
public interface LockHandler<T> {

     T handle() throws Throwable;
}