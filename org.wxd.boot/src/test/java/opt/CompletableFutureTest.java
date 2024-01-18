package opt;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.wxd.boot.threading.Executors;
import org.wxd.boot.threading.OptFuture;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

@Slf4j
public class CompletableFutureTest {

    @Test
    public void t1() {
        Supplier<Long> supplier = new Supplier<>() {
            @Override public Long get() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return 1L;
            }
        };
        // CompletableFuture.supplyAsync(supplier, Executors.getVTExecutor());
        OptFuture<Long> completable = Executors.getVTExecutor().optFuture(supplier);
        completable.peek(v -> System.out.println(v));
        while (true) ;
    }

    @Test
    public void tm3() throws InterruptedException {
        CompletableFuture.supplyAsync(() -> {
                    throw new RuntimeException("1");
                })
                .thenApply(o -> {
                    System.out.println(o);
                    return "2";
                })
                .thenApply(o -> {
                    System.out.println(o);
                    return o;
                })
                .exceptionallyAsync(throwable -> {
                    log.info("3", throwable);
                    return null;
                });
        Thread.sleep(100);
    }

    @Test
    public void c12() throws IOException {
        OptFuture<Void> stringCompletableFuture = OptFuture.empty();
        stringCompletableFuture.complete(null);
        stringCompletableFuture.whenComplete((v, throwable) -> log.info("{}", v, throwable));
        System.in.read();
    }

    @Test
    public void c13() throws IOException {
        OptFuture<String> stringCompletableFuture = Executors.getVTExecutor().optFuture(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return "1";
        });
        stringCompletableFuture.peek(System.out::println);
        System.in.read();
    }

    @Test
    public void c14() throws IOException {
        OptFuture<String> stringCompletableFuture = Executors.getVTExecutor().optFuture(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return "1";
        });
        stringCompletableFuture.peek(System.out::println);
        System.in.read();
    }

    @Test
    public void c1() {
        CompletableFuture<String> stringCompletableFuture = new CompletableFuture<>();
        stringCompletableFuture.thenAccept(s -> System.out.println(s))
                .exceptionally((throwable) -> {
                    throwable.printStackTrace(System.out);
                    return null;
                });
        stringCompletableFuture.complete("1");
    }

    @Test
    public void c2() throws ExecutionException, InterruptedException {
        CompletableFuture<String> stringCompletableFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            throw new RuntimeException("1");
        });

        stringCompletableFuture
                .thenAccept(s -> System.out.println(s))
                .exceptionally((throwable) -> {
                    log.error("", throwable);
                    return null;
                });
        Thread.sleep(1000);
    }
}
