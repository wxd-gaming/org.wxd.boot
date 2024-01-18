package org.wxd.boot.threading;

import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.exception.Throw;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 异步类
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2024-01-15 17:57
 **/
@Slf4j
public class OptCall<T> implements Job {

    final Job job;
    final CountDownLatch count = new CountDownLatch(1);
    final AtomicReference<Object> tAtomicReference = new AtomicReference<>();

    public OptCall(Job job) {
        this.job = job;
    }

    public void complete(T t) {
        tAtomicReference.set(t);
        count.countDown();
    }

    public void completeExceptionally(Throwable throwable) {
        tAtomicReference.set(throwable);
        count.countDown();
    }

    public T get() {
        try {
            if (count.getCount() > 0) count.await();
            Object poll = tAtomicReference.get();
            if (poll instanceof Throwable throwable) {
                throw Throw.as(throwable);
            }
            return (T) poll;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public T get(long time, TimeUnit timeUnit) {
        try {
            if (count.getCount() > 0) {
                boolean await = count.await(time, timeUnit);
                if (!await) {
                    throw new RuntimeException("time out");
                }
            }
            Object poll = tAtomicReference.get();
            if (poll instanceof Throwable throwable) {
                throw Throw.as(throwable);
            }
            return (T) poll;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public boolean cancel() {
        return job.cancel();
    }

}
