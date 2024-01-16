package org.wxd.boot.threading;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.*;

/**
 * 异步类
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2024-01-15 17:57
 **/
@Slf4j
public class OptFuture<T> {

    /** 创建数据 */
    public static <U> OptFuture<U> empty() {
        return new OptFuture<>(null);
    }

    /** 创建数据 */
    public static <U> OptFuture<U> create(U u) {
        return new OptFuture<>(u);
    }

    /** 创建数据 */
    public static <U> OptFuture<U> create(Supplier<U> u) {
        return new OptFuture<>(u.get());
    }

    /** 创建异步获取数据 */
    public static <U> OptFuture<U> createAsync(Supplier<U> supplier) {
        return new OptFuture<>(supplier, Executors.getVTExecutor(), 5);
    }

    /** 创建异步获取数据 */
    public static <U> OptFuture<U> createAsync(Supplier<U> supplier, IExecutorServices executorServices) {
        return new OptFuture<>(supplier, executorServices, 5);
    }

    private ReentrantLock reentrantLock = null;
    private boolean onComplete = false;
    private BlockingQueue<T> data = new LinkedBlockingDeque<>(1);
    private AtomicReference<Throwable> exception = new AtomicReference<>();
    private AtomicReference<Runnable> runnable = new AtomicReference<>();
    private OptFuture next = null;

    protected OptFuture(Supplier<T> supplier, IExecutorServices executorServices, int stack) {
        this.reentrantLock = new ReentrantLock();
        executorServices.submit(new Event() {
            @Override public void onEvent() throws Exception {
                try {
                    complete(supplier.get());
                } catch (Throwable throwable) {
                    completeExceptionally(throwable);
                }
            }
        }, stack);
    }

    public OptFuture(T t) {
        this.reentrantLock = new ReentrantLock();
        if (t != null) {
            this.data.add(t);
        }
    }

    public OptFuture(ReentrantLock reentrantLock, BlockingQueue<T> data, AtomicReference<Throwable> exception) {
        this.reentrantLock = reentrantLock;
        this.data = data;
        this.exception = exception;
    }

    public OptFuture<T> complete(T t) {
        this.reentrantLock.lock();
        try {
            if (t != null) {
                this.data.add(t);
            }
            this.onComplete = true;
            doAction();
            return this;
        } finally {
            this.reentrantLock.unlock();
        }
    }

    public OptFuture<T> completeExceptionally(Throwable throwable) {
        this.reentrantLock.lock();
        try {
            this.exception.set(throwable);
            this.onComplete = true;
            doAction();
            return this;
        } finally {
            this.reentrantLock.unlock();
        }
    }

    /** 当未查找到数据，并且无异常的情况下，赋值给定值 */
    public OptFuture<T> orComplete(Supplier<T> supplier) {
        this.reentrantLock.lock();
        try {
            set(() -> {
                try {
                    if (exception.get() == null && data.isEmpty()) {
                        data.add(supplier.get());
                    }
                } catch (Throwable throwable) {
                    this.exception.set(throwable);
                }
            });
            OptFuture<T> tOptFuture = new OptFuture<>(this.reentrantLock, this.data, this.exception);
            this.next = tOptFuture;
            doAction();
            return tOptFuture;
        } finally {
            this.reentrantLock.unlock();
        }
    }

    /** 数据转换 */
    public <U> OptFuture<U> map(Function<T, U> function) {
        this.reentrantLock.lock();
        try {
            OptFuture<U> uOptFuture = OptFuture.empty();
            set(() -> {
                try {
                    T tmp = data.poll();
                    if (exception.get() != null) {
                        uOptFuture.exception.set(exception.get());
                    } else {
                        uOptFuture.data.add(function.apply(tmp));
                    }
                } catch (Throwable throwable) {
                    uOptFuture.exception.set(throwable);
                }
            });
            this.next = uOptFuture;
            doAction();
            return uOptFuture;
        } finally {
            this.reentrantLock.unlock();
        }
    }

    /** 数据过滤 */
    public OptFuture<T> filter(Predicate<T> predicate) {
        this.reentrantLock.lock();
        try {
            OptFuture<T> uOptFuture = OptFuture.empty();
            set(() -> {
                try {
                    T tmp = data.poll();
                    if (exception.get() == null && tmp != null) {
                        if (predicate.test(tmp)) {
                            uOptFuture.data.add(tmp);
                        }
                        uOptFuture.exception.set(null);
                    }
                } catch (Throwable throwable) {
                    this.exception.set(throwable);
                }
            });
            this.next = uOptFuture;
            doAction();
            return uOptFuture;
        } finally {
            this.reentrantLock.unlock();
        }
    }

    /** 查看数据 */
    public OptFuture<T> peek(Consumer<T> consumer) {
        this.reentrantLock.lock();
        try {
            OptFuture<T> tOptFuture = new OptFuture<>(this.reentrantLock, this.data, this.exception);
            set(() -> {
                try {
                    T tmp = data.peek();
                    if (exception.get() == null && tmp != null) {
                        consumer.accept(tmp);
                    }
                } catch (Throwable throwable) {
                    this.exception.set(throwable);
                }
            });
            this.next = tOptFuture;
            doAction();
            return tOptFuture;
        } finally {
            this.reentrantLock.unlock();
        }
    }

    /** 消费订阅,消费后就删除 */
    public OptFuture<T> subscribe(Consumer<T> consumer) {
        this.reentrantLock.lock();
        try {
            set(() -> {
                try {
                    T tmp = data.poll();
                    if (exception.get() == null && tmp != null) {
                        consumer.accept(tmp);
                    }
                } catch (Throwable throwable) {
                    this.exception.set(throwable);
                }
            });
            OptFuture<T> tOptFuture = new OptFuture<>(this.reentrantLock, this.data, this.exception);
            this.next = tOptFuture;
            doAction();
            return tOptFuture;
        } finally {
            this.reentrantLock.unlock();
        }
    }

    /** 当完成之后 自定判定内容是否 null 异常 */
    public OptFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        this.reentrantLock.lock();
        try {
            set(() -> {
                try {
                    T tmp = data.poll();
                    action.accept(tmp, exception.get());
                } catch (Throwable throwable) {
                    this.exception.set(throwable);
                }
            });
            OptFuture<T> tOptFuture = new OptFuture<>(this.reentrantLock, this.data, this.exception);
            this.next = tOptFuture;
            doAction();
            return tOptFuture;
        } finally {
            this.reentrantLock.unlock();
        }
    }

    /** 增加异常处理 */
    public OptFuture<T> onError() {
        return onError(throwable -> {log.info("", throwable);});
    }

    /** 增加异常处理 */
    public OptFuture<T> onError(Consumer<Throwable> consumer) {
        this.reentrantLock.lock();
        try {
            set(() -> {
                try {
                    Throwable tmp = exception.get();
                    exception.set(null);
                    if (tmp != null) {
                        consumer.accept(tmp);
                    }
                } catch (Throwable throwable) {
                    this.exception.set(throwable);
                }
            });
            OptFuture<T> tOptFuture = new OptFuture<>(this.reentrantLock, this.data, this.exception);
            this.next = tOptFuture;
            doAction();
            return tOptFuture;
        } finally {
            this.reentrantLock.unlock();
        }
    }

    public boolean isEmpty() {
        return get() == null;
    }

    public T get() {
        return get(-1, TimeUnit.MILLISECONDS);
    }

    public T get(long timeout, TimeUnit unit) {
        this.reentrantLock.lock();
        try {
            OptCall<T> optCall = new OptCall<>(null);
            set(() -> {
                Throwable throwable = exception.get();
                if (throwable != null) {
                    optCall.completeExceptionally(throwable);
                } else {
                    optCall.complete(this.data.poll());
                }
            });
            doAction();
            if (timeout == -1) {
                return optCall.get();
            } else {
                return optCall.get(timeout, unit);
            }
        } finally {
            this.reentrantLock.unlock();
        }
    }

    public T orElse(T t) {
        T t1 = get();
        if (t1 != null) return t1;
        return t;
    }

    void set(Runnable runnable) {
        if (this.runnable.get() != null) throw new RuntimeException("重复调用");
        this.runnable.set(runnable);
    }

    void doAction() {
        this.reentrantLock.lock();
        try {
            if (!onComplete) return;
            if (this.runnable.get() != null) {
                //log.info("{}", this.name);
                this.runnable.get().run();
                if (this.next != null) {
                    this.next.onComplete = true;
                    this.next.doAction();
                }
            }
        } finally {
            this.reentrantLock.unlock();
        }
    }

}
