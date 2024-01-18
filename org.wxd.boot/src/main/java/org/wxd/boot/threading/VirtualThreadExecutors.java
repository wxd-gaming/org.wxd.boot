package org.wxd.boot.threading;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义线程池, 会缓存线程，避免频繁的生成线程
 * <p>
 * 禁止使用 synchronized 同步锁
 * <p>
 * 直接线程池，每一个任务都会new Virtual Thread
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-04-27 10:34
 **/
@Slf4j
@Getter
public final class VirtualThreadExecutors implements Executor {

    final Thread.Builder.OfVirtual ofVirtual;
    final String name;
    final int coreSize;
    final int maxSize;
    final BlockingQueue<Runnable> queue;
    /** 当前激活的线程数量 */
    AtomicInteger threadActivationCount = new AtomicInteger();
    /** 正在关闭 */
    AtomicBoolean shutdowning = new AtomicBoolean();
    /** 已经关闭 */
    AtomicBoolean shutdown = new AtomicBoolean();
    /** 正在终止 */
    AtomicBoolean terminating = new AtomicBoolean();
    /** 已经终止 */
    AtomicBoolean terminate = new AtomicBoolean();

    public VirtualThreadExecutors(String name, int coreSize) {
        this(name, coreSize, coreSize);
    }

    public VirtualThreadExecutors(String name, int coreSize, int maxSize) {
        this(name, coreSize, maxSize, new LinkedBlockingQueue<>(Integer.MAX_VALUE));
    }

    public VirtualThreadExecutors(String name, int coreSize, int maxSize, BlockingQueue<Runnable> queue) {
        if (!name.startsWith("vt-"))
            name = "vt-" + name;
        this.name = name;
        this.coreSize = coreSize;
        this.maxSize = maxSize;
        this.queue = queue;
        this.ofVirtual = Thread.ofVirtual().name(this.name + "-", 1);
    }

    @Override public void execute(Runnable command) {
        if (shutdowning.get() || this.terminating.get())
            throw new RuntimeException("线程正在关闭");
        queue.add(command);
        checkThread();
    }

    void checkThread() {
        if (this.terminating.get()) {
            /*正在终止*/
            if (threadActivationCount.get() <= 0) {
                over();
            }
            return;
        }
        if (threadActivationCount.get() >= maxSize) return;
        if (threadActivationCount.get() < coreSize || queue.size() > threadActivationCount.get() * 20) {
            Runnable runnable = queue.poll();
            if (runnable == null) return;
            Runnable threadRun = () -> {
                try {
                    try {
                        runnable.run();
                    } catch (Throwable throwable) {
                        log.error("执行器异常 {}", runnable, throwable);
                    } finally {
                        threadActivationCount.decrementAndGet();
                        checkThread();
                    }
                } catch (Throwable throwable) {/*不能加东西，log也有可能异常*/}
            };
            threadActivationCount.incrementAndGet();
            Thread start = ofVirtual.start(threadRun);
        }
    }

    /** 即将关闭线程，不再接受新任务，并且线程当前任务执行完成不再执行队列里面的任务 */
    public List<Runnable> terminate() {
        this.shutdowning.set(true);
        this.terminating.set(true);
        while (!isTerminated()) {}
        List<Runnable> tasks = new ArrayList<>(queue);
        queue.clear();
        return tasks;
    }

    /** 已经关闭完成 */
    public boolean isTerminated() {
        return terminate.get();
    }

    /** 准备关闭，不再接受新的任务 ,并且会等待当前队列任务全部执行完成 */
    public void shutdown() {
        this.shutdowning.set(true);
        while (!isTerminated() && threadActivationCount.get() >0) {}
        over();
    }

    /** 即将关闭线程状态 */
    public boolean isShutdown() {
        return this.shutdown.get();
    }

    void over() {
        this.terminate.set(true);
        this.shutdown.set(true);
    }

}
