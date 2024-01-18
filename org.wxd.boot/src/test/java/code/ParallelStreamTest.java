package code;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.wxd.boot.collection.OfList;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-01-26 17:09
 **/
@Slf4j
public class ParallelStreamTest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Test
    public void t1() {
        parallelStream();
        parallelStream();
        parallelStream();
        parallelStream();
        stream();
        stream();
        stream();
        stream();
        stream();
        System.out.println(1);
    }

    public void parallelStream() {
        List<Integer> list = OfList.asList(1, 2, 3, 4, 5);
        final long nanoTime = System.nanoTime();
        list.parallelStream().forEach(v -> {
            list.parallelStream().forEach(i -> log.warn(Thread.currentThread().getId() + ", " + String.valueOf(i)));
        });
        System.out.println("parallelStream() 耗时：" + ((System.nanoTime() - nanoTime) / 10000 / 100f));
    }

    public void stream() {
        List<Integer> list = OfList.asList(1, 2, 3, 4, 5);
        final long nanoTime = System.nanoTime();
        list.stream().forEach(v -> {
            list.stream().forEach(i -> log.warn(Thread.currentThread().getId() + ", " + String.valueOf(i)));
        });
        System.out.println("stream() 耗时：" + ((System.nanoTime() - nanoTime) / 10000 / 100f));
        list.stream().peek(i -> log.warn(Thread.currentThread().getId() + ", " + String.valueOf(i))).collect(Collectors.toList());
    }

}
