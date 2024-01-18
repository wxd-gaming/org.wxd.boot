package code;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.wxd.boot.threading.Executors;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 异步测试
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-10-24 16:30
 **/
@Slf4j
public class AsyncTest {

    /**
     * thenAccept子任务和父任务公用同一个线程
     */
    @Test
    public void thenRunAsync() throws Exception {
        for (int i = 1; i <= 1; i++) {
            t(i);
        }
        Thread.sleep(3000);
    }

    public void t(int f) {
        CompletableFuture.runAsync(() -> {
                    log.info("{}", 1);
                    throw new RuntimeException("1");
                })
                .thenApply((v) -> {
                    log.info("{}", 2);
                    throw new RuntimeException("2");
                })
                .whenComplete((s, throwable) -> log.info("whenComplete {}", s, throwable))
                .exceptionally(throwable -> {
                    if (throwable instanceof java.util.concurrent.CompletionException) {
                        throwable = throwable.getCause();
                    }
                    log.error("exceptionally ", throwable);
                    return null;
                });
    }

    @Test
    public void te() {
        CompletableFuture
                .supplyAsync(() -> new RuntimeException("1"))
                .thenAccept(System.out::println)
                .exceptionally(throwable -> {
                    throwable.printStackTrace(System.out);
                    return null;
                });
    }

    @Test
    public void streamWhile() {
        Consumer<Stream<Integer>> action = integerStream -> System.out.println(integerStream.map(Object::toString).collect(Collectors.joining(",")));
        {
            Stream<Integer> stream = List.of(1, 3, 5, 8, 10, 20, 35, 2, 5, 7).stream();
            action.accept(stream.takeWhile(num -> num <= 20));
        }
        {
            Stream<Integer> stream = List.of(1, 3, 5, 8, 10, 20, 35, 2, 5, 7).stream();
            action.accept(stream.dropWhile(num -> num <= 20));
        }
        {
            Stream<Integer> stream = new LinkedHashSet<>(List.of(1, 3, 8, 10, 20, 35, 2, 5, 7)).stream();
            action.accept(stream.takeWhile(num -> num <= 20));
        }
        {
            Stream<Integer> stream = new LinkedHashSet<>(List.of(1, 3, 8, 10, 20, 35, 2, 5, 7)).stream();
            action.accept(stream.dropWhile(num -> num <= 20));
        }
        {
            Stream<Integer> stream = new HashSet<>(List.of(1, 3, 8, 10, 20, 35, 2, 5, 7)).stream();
            action.accept(stream.takeWhile(num -> num <= 20));
        }
        {
            Stream<Integer> stream = new HashSet<>(List.of(1, 3, 8, 10, 20, 35, 2, 5, 7)).stream();
            action.accept(stream.dropWhile(num -> num <= 20));
        }
        {
            Stream<Integer> stream = new TreeSet<>(List.of(1, 3, 8, 10, 20, 35, 2, 5, 7)).stream();
            action.accept(stream.takeWhile(num -> num <= 20));
        }
        {
            Stream<Integer> stream = new TreeSet<>(List.of(1, 3, 8, 10, 20, 35, 2, 5, 7)).stream();
            action.accept(stream.dropWhile(num -> num <= 20));
        }
    }

    @Test
    public void executor() {
        CompletableFuture.runAsync(
                () -> System.out.println(Thread.currentThread().getName() + " - 1"),
                Executors.getDefaultExecutor()
        );
    }

}
