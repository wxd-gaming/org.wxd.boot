package demo;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;

/**
 * Ces
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-12-28 10:50
 **/
public class CompletableTest {

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
    public void c2() {
        CompletableFuture<String> stringCompletableFuture = new CompletableFuture<>();
        stringCompletableFuture.thenAccept(s -> System.out.println(s))
                .exceptionally((throwable) -> {
                    throwable.printStackTrace(System.out);
                    return null;
                });
        stringCompletableFuture.completeExceptionally(new RuntimeException("1"));
    }

}
