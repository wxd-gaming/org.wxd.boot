package code;

import org.junit.Test;
import org.wxd.boot.threading.Executors;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

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
        CompletableFuture<Long> completable = Executors.getDefaultExecutor().completableFuture(supplier);
        completable.thenAccept(v -> System.out.println(v));
        while (true) ;
    }

}
