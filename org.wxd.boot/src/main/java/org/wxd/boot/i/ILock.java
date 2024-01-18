package org.wxd.boot.i;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-04-26 18:20
 **/
public interface ILock {

    /**
     * 读取锁
     */
    ReentrantLock getLock();

    default void lockRun(Runnable runnable) {
        lock();
        try {
            runnable.run();
        } finally {
            unlock();
        }
    }

    default <R> R lockRun(Supplier<R> runnable) {
        lock();
        try {
            return runnable.get();
        } finally {
            unlock();
        }
    }

    /**
     * 读取锁
     */
    default void lock() {
        try {
            if (!getLock().isHeldByCurrentThread()) {
                getLock().lockInterruptibly();
            }
        } catch (InterruptedException e) {
            /*如果是线程终止，继续终止线程*/
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 读取锁
     */
    default void unlock() {
        if (getLock().isHeldByCurrentThread()) {
            getLock().unlock();
        }
    }

}
