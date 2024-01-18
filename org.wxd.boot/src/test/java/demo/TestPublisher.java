package demo;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.wxd.boot.publisher.Flux;

import java.util.List;

/**
 * 测试mono
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-12-21 09:43
 **/
@Slf4j
public class TestPublisher {

    @Test
    public void t1() throws Exception {

        Flux.createAsync(() -> {
                    try {
                        Thread.sleep(1000);
                        return List.of(1, 2, 3, 4);
                    } catch (InterruptedException e) {throw new RuntimeException(e);}
                })
                .subscribe(v -> log.debug("{}", v))
                .map(i -> "我是Flux：" + i)
                .orComplete(() -> List.of("我是特殊值 Flux"))
                .subscribe(v -> log.debug("{}", v))
                .map(v -> {throw new RuntimeException("执行异常");})
                .onError(throwable -> log.debug("onError", throwable))
                .whenComplete((t, throwable) -> {
                    log.debug("whenComplete {}", t, throwable);
                })
        ;

        Flux.create(List.of(List.of(1, 2, 3), List.of(7, 8, 9)))
                .subscribe(v -> log.debug("{}", v))
                .flatMap(v -> v.stream())
                .map(i -> "我是Flux：" + i)
                .orComplete(() -> List.of("我是特殊值 Flux"))
                .subscribe(v -> log.debug("{}", v))
                .map(v -> {throw new RuntimeException("执行异常");})
                .onError(throwable -> log.debug("onError", throwable))
                .whenComplete((t, throwable) -> {
                    log.debug("whenComplete {}", t, throwable);
                })
        ;

        int l = 10;
        while (l-- > 0) {
            Thread.sleep(300);
            log.debug("主线程");
        }

    }
}
