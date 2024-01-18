package opt;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.wxd.boot.threading.Executors;
import org.wxd.boot.threading.OptFuture;

/**
 * 测试
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2024-01-15 18:08
 **/
@Slf4j
public class OptFutureTest {

    @Test
    public void o1() {
        OptFuture<Integer> optFuture = OptFuture.empty();
        optFuture
                .subscribe(s -> log.info("{}", s))
                .subscribe(s -> log.info("{}", s))
                .onError(throwable -> {
                    log.error("{}", throwable.getMessage());
                });
        //optFuture.complete(1);
        optFuture.completeExceptionally(new RuntimeException("1"));
    }

    @Test
    public void o2() throws Exception {
        for (int i = 0; i < 2; i++) {
            final int fi = i;
            OptFuture<String> async = OptFuture.createAsync(() -> {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {

                }
                return String.valueOf(fi);
            });
            String s1 = async
                    .subscribe(s -> {
                        System.out.println(s);
                        throw new RuntimeException(fi + "-2");
                    })
                    .onError(throwable -> log.info("{}", throwable.getMessage()))
                    .orComplete(() -> String.valueOf("33"))
                    .peek(log::info)
                    .get();
            log.info(s1);
        }
        Thread.sleep(3000);
    }

    @Test
    public void o3() throws Exception {
        OptFuture<Integer> optFuture = OptFuture.empty();
        Executors.getLogicExecutor().submit(() -> {
            try {
                //Thread.sleep(5);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            optFuture.complete(1);
        });
        OptFuture<String> map = optFuture
                //.complete(1)
                .peek(s -> log.info("{}", s))
                .peek(s -> log.info("{}", s))
                .peek(s -> log.info("{}", s))
                .map(s -> "map-" + s)
                .filter(s -> s.startsWith("map"))
                .peek(s -> log.info("{}", s))
                .map(s -> "map-" + s)
                .peek(s -> log.info("{}", s))
                .map(s -> "map-" + s)
                .peek(s -> log.info("{}", s))
                .map(s -> "map-" + s)
                .peek(s -> log.info("{}", s))
                .map(s -> "map-" + s)
                .peek(s -> log.info("{}", s))
                .peek(s -> log.info("{}", s));

        Executors.getLogicExecutor().submit(() -> {
            try {
                Thread.sleep(5);
                map
                        .map(s -> "end")
                        .peek(s -> log.info("{}", s));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        //optFuture.complete(1);
        //opt.completeExceptionally(new RuntimeException("1"));
        Thread.sleep(3000);
    }
}
