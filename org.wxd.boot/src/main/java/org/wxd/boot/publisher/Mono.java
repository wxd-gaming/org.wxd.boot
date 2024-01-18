package org.wxd.boot.publisher;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.threading.Executors;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

/**
 * 单数据异步编程
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-12-21 09:34
 **/
@Slf4j
@Getter
public class Mono<T> {

    protected final CompletableFuture<T> completableFuture;

    public Mono(CompletableFuture<T> completableFuture) {
        this.completableFuture = completableFuture;
    }

    /** 创建数据 */
    public static <U> Mono<U> create(U u) {
        Mono<U> uMono = new Mono<>(new CompletableFuture<>());
        uMono.completableFuture.complete(u);
        return uMono;
    }

    /** 创建异步获取数据 */
    public static <U> Mono<U> createAsync(Supplier<U> supplier) {
        return new Mono<>(CompletableFuture.supplyAsync(supplier, Executors.getVTExecutor()));
    }

    /** 当未查找到数据，并且无异常的情况下，赋值给定值 */
    public Mono<T> orComplete(Supplier<T> supplier) {
        return new Mono<>(completableFuture.thenApply((t) -> {
            if (t != null)
                return t;
            else
                return supplier.get();
        }));
    }

    /** 数据转换 */
    public <U> Mono<U> map(Function<T, U> function) {
        return new Mono<>(completableFuture.thenApply(t -> {
            if (t != null) return function.apply(t);
            else return null;
        }));
    }

    /** 数据过滤 */
    public Mono<T> filter(Predicate<T> predicate) {
        return new Mono<>(completableFuture.thenApply(t -> {
            if (predicate.test(t)) return t;
            else return null;
        }));
    }

    /** 消费订阅 */
    public Mono<T> subscribe(Consumer<T> consumer) {
        return new Mono<>(completableFuture.thenApply(v -> {
            if (v == null) return null;
            consumer.accept(v);
            return v;
        }));
    }

    /** 当完成之后 自定判定内容是否 null 异常 */
    public Mono<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return new Mono<>(completableFuture.whenComplete(action));
    }

    /** 增加异常处理 */
    public Mono<T> onError() {
        return onError(throwable -> {log.info("", throwable);});
    }

    /** 增加异常处理 */
    public Mono<T> onError(Consumer<Throwable> consumer) {
        return new Mono<>(completableFuture.exceptionally((throwable) -> {
            consumer.accept(throwable);
            return null;
        }));
    }

    public boolean isEmpty() {
        return get() == null;
    }

    public T orElse(T t) {
        T t1 = get();
        if (t1 != null) return t1;
        return t;
    }

    public T get() {
        try {
            return completableFuture.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public T get(long timeout, TimeUnit unit) {
        try {
            return completableFuture.get(timeout, unit);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
