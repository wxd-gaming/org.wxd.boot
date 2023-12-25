package org.wxd.boot.threading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wxd.boot.str.StringUtil;
import org.wxd.boot.system.GlobalUtil;

import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

public interface IExecutorServices extends Executor {

    Logger log = LoggerFactory.getLogger(IExecutorServices.class);

    /** 线程池名称 */
    String getName();

    int getCoreSize();

    int getMaxSize();

    /** 当前线程池触发警告队列长度 */
    long getQueueCheckSize();

    /** 当前线程池触发警告队列长度 */
    IExecutorServices setQueueCheckSize(long queueCheckSize);

    /** 当前线程池队列是否为空 */
    boolean isQueueEmpty();

    /** 当前线程池队列长度 */
    int queueSize();

    /** 关闭 */
    void shutdown();

    /** 关闭 */
    List<Runnable> terminate();

    boolean isShutdown();

    boolean isTerminated();

    ConcurrentHashMap<String, ExecutorQueue> getExecutorQueueMap();

    /** 提交到线程 */
    void threadPoolExecutor(Runnable command);

    /** 线程池队列 */
    BlockingQueue<Runnable> threadPoolQueue();

    /** 普通任务 */
    @Override default void execute(Runnable runnable) {
        String queueName = null;
        if (runnable instanceof EventRunnable eventRunnable) {
            queueName = eventRunnable.getQueueName();
        }
        int stackTrace = 3;
        if (runnable instanceof ForkJoinTask)
            stackTrace = 6;
        submit(queueName, runnable, stackTrace);
    }

    /** 普通任务 */
    default Job submit(Runnable runnable) {
        String queueName = null;
        if (runnable instanceof EventRunnable eventRunnable) {
            queueName = eventRunnable.getQueueName();
        }
        return submit(queueName, runnable, 3);
    }

    /** 普通任务 */
    default Job submit(Runnable runnable, int stackTrace) {
        String queueName = null;
        if (runnable instanceof EventRunnable eventRunnable) {
            queueName = eventRunnable.getQueueName();
        }
        return submit(queueName, runnable, stackTrace);
    }

    /** 队列任务 */
    default Job submit(String queueName, Runnable command) {
        return submit(queueName, command, 3);
    }

    /**
     * 队列任务
     *
     * @param queueName  队列名
     * @param runnable   需要执行的任务
     * @param stackTrace 记录来源堆栈层级
     * @return
     */
    default Job submit(String queueName, Runnable runnable, int stackTrace) {
        ExecutorServiceJob executorServiceJob = new ExecutorServiceJob(this, runnable, stackTrace);
        executeJob(queueName, executorServiceJob);
        return executorServiceJob;
    }

    /**
     * 提交带回调的执行
     *
     * @param runnable 需要执行的任务
     * @param v        给定的返回值
     * @param <V>
     * @return
     */
    default <V> Future<V> submitCall(Runnable runnable, V v) {
        return submitCall(runnable, v, 3);
    }

    /**
     * 提交带回调的执行
     *
     * @param runnable   需要执行的任务
     * @param v          给定的返回值
     * @param stackTrace 记录来源堆栈层级
     * @param <V>
     * @return
     */
    default <V> Future<V> submitCall(Runnable runnable, V v, int stackTrace) {
        FutureTask<V> task = new FutureTask<>(runnable, v);
        ExecutorServiceJob executorServiceJob = new ExecutorServiceJob(this, task, stackTrace);
        executeJob(null, executorServiceJob);
        return task;
    }

    /** 提交带回调的执行 */
    default <V> Future<V> submitCall(Callable<V> callable) {
        return submitCall(callable, 3);
    }

    /**
     * 提交带回调的执行
     *
     * @param callable   执行带回调等待
     * @param stackTrace 记录来源堆栈层级
     * @param <V>
     * @return
     */
    default <V> Future<V> submitCall(Callable<V> callable, int stackTrace) {
        FutureTask<V> task = new FutureTask<>(callable);
        ExecutorServiceJob executorServiceJob = new ExecutorServiceJob(this, task, stackTrace);
        executeJob(null, executorServiceJob);
        return task;
    }

    /**
     * 提交带回调的执行
     *
     * @param queueName 队列名称
     * @param runnable  需要执行的任务
     * @param v         给定的返回值
     * @param <V>
     * @return
     */
    default <V> Future<V> submitCall(String queueName, Runnable runnable, V v) {
        return submitCall(queueName, runnable, v, 3);
    }

    /**
     * 提交带回调的执行
     *
     * @param queueName  队列名称
     * @param runnable   需要执行的任务
     * @param v          给定的返回值
     * @param stackTrace 堆栈层级
     * @param <V>
     * @return
     */
    default <V> Future<V> submitCall(String queueName, Runnable runnable, V v, int stackTrace) {
        FutureTask<V> task = new FutureTask<>(runnable, v);
        ExecutorServiceJob executorServiceJob = new ExecutorServiceJob(this, task, stackTrace);
        executeJob(queueName, executorServiceJob);
        return task;
    }

    /** 提交带回调的执行 */
    default <V> Future<V> submitCall(String queueName, Callable<V> callable) {
        return submitCall(queueName, callable, 3);
    }

    /** 提交带回调的执行 */
    default <V> Future<V> submitCall(String queueName, Callable<V> callable, int stackTrace) {
        FutureTask<V> task = new FutureTask<>(callable);
        ExecutorServiceJob executorServiceJob = new ExecutorServiceJob(this, task, stackTrace);
        executeJob(queueName, executorServiceJob);
        return task;
    }

    /** 提交带回调的执行 */
    default <V> CompletableFuture<V> completableFuture(Supplier<V> supplier) {
        return CompletableFuture.supplyAsync(supplier, this);
    }

    /** 提交带回调的执行 */
    default CompletableFuture<Void> completableFuture(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, this);
    }

    /** 执行一次的延时任务 */
    default TimerJob schedule(Runnable command, long delay, TimeUnit unit) {
        return schedule(command, delay, unit, 3);
    }

    /** 执行一次的延时任务 */
    default TimerJob schedule(Runnable command, long delay, TimeUnit unit, int stackTrace) {
        String queueName = null;
        if (command instanceof EventRunnable eventRunnable) {
            queueName = eventRunnable.getQueueName();
        }
        ExecutorServiceJob executorServiceJob = new ExecutorServiceJob(this, command, stackTrace);
        TimerJob timerJob = new TimerJob(this, queueName, executorServiceJob, delay, delay, unit, 1);
        Executors.TIMER_THREAD.add(timerJob);
        return timerJob;
    }

    /** 依赖队列的执行一次的延时任务 */
    default TimerJob schedule(String queueName, Runnable command, long delay, TimeUnit unit) {
        ExecutorServiceJob executorServiceJob = new ExecutorServiceJob(this, command, 2);
        TimerJob timerJob = new TimerJob(this, queueName, executorServiceJob, delay, delay, unit, 1);
        Executors.TIMER_THREAD.add(timerJob);
        return timerJob;
    }

    /** 定时运行可取消的周期性任务 上一次没有执行，不会执行第二次，等待上一次执行完成 */
    default TimerJob scheduleAtFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        String queueName = null;
        if (command instanceof EventRunnable eventRunnable) {
            queueName = eventRunnable.getQueueName();
        }
        ExecutorServiceJob executorServiceJob = new ExecutorServiceJob(this, command, 2);
        TimerJob timerJob = new TimerJob(this, queueName, executorServiceJob, initialDelay, delay, unit, -1);
        Executors.TIMER_THREAD.add(timerJob);
        return timerJob;
    }

    /** 定时运行可取消的周期性任务 上一次没有执行，不会执行第二次，等待上一次执行完成 */
    default TimerJob scheduleAtFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit, int execCount) {
        return scheduleAtFixedDelay(command, initialDelay, delay, unit, execCount, 3);
    }

    default TimerJob scheduleAtFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit, int execCount, int stackTrace) {
        String queueName = null;
        if (command instanceof EventRunnable eventRunnable) {
            queueName = eventRunnable.getQueueName();
        }
        ExecutorServiceJob executorServiceJob = new ExecutorServiceJob(this, command, stackTrace);
        TimerJob timerJob = new TimerJob(this, queueName, executorServiceJob, initialDelay, delay, unit, execCount);
        Executors.TIMER_THREAD.add(timerJob);
        return timerJob;
    }

    /** 依赖队列 定时运行可取消的周期性任务 上一次没有执行，不会执行第二次，等待上一次执行完成 */
    default TimerJob scheduleAtFixedDelay(String queueName, Runnable command, long initialDelay, long delay, TimeUnit unit) {
        ExecutorServiceJob executorServiceJob = new ExecutorServiceJob(this, command, 2);
        TimerJob timerJob = new TimerJob(this, queueName, executorServiceJob, initialDelay, delay, unit, -1);
        Executors.TIMER_THREAD.add(timerJob);
        return timerJob;
    }

    /** 依赖队列 定时运行可取消的周期性任务 上一次没有执行，不会执行第二次，等待上一次执行完成 */
    default TimerJob scheduleAtFixedDelay(String queueName, Runnable command, long initialDelay, long delay, TimeUnit unit, int execCount) {
        return scheduleAtFixedDelay(queueName, command, initialDelay, delay, unit, execCount, 3);
    }

    /** 依赖队列 定时运行可取消的周期性任务 上一次没有执行，不会执行第二次，等待上一次执行完成 */
    default TimerJob scheduleAtFixedDelay(String queueName, Runnable command, long initialDelay, long delay, TimeUnit unit, int execCount, int stackTrace) {
        ExecutorServiceJob executorServiceJob = new ExecutorServiceJob(this, command, stackTrace);
        TimerJob timerJob = new TimerJob(this, queueName, executorServiceJob, initialDelay, delay, unit, execCount);
        Executors.TIMER_THREAD.add(timerJob);
        return timerJob;
    }

    default void executeJob(String queueName, ExecutorServiceJob job) {
        if (isShutdown() && !isTerminated()) {
            throw new RuntimeException("线程正在关闭 " + job);
        }
        if (isTerminated()) {
            throw new RuntimeException("线程已经关闭 " + job);
        }
        job.queueName = queueName;
        /**定时器任务，需要重置一次*/
        job.initTaskTime = System.nanoTime();
        job.append.set(true);
        if (StringUtil.notEmptyOrNull(queueName)) {
            ExecutorQueue executorQueue = getExecutorQueueMap().computeIfAbsent(queueName, k -> new ExecutorQueue(this, k));
            executorQueue.add(job);
        } else {
            threadPoolExecutor(job);
            int queueSize = queueSize();
            if (queueSize > getQueueCheckSize()) {
                RuntimeException throwable = new RuntimeException();
                log.error("任务剩余过多 主队列 size：" + queueSize, throwable);
                GlobalUtil.exception("任务剩余过多 主队列 size：" + queueSize, throwable);
            }
        }
    }

}
