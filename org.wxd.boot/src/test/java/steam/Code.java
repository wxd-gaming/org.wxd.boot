package steam;

import org.junit.Test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-05-16 17:20
 **/
public class Code implements Serializable {

    /*调试stream的执行流程*/
    @Test
    public void test() {
        List<ObjectOne> list = Arrays.asList(new ObjectOne().setI1(1), new ObjectOne().setI2(1));
        final long count = list.stream()
                .filter(v -> {
                    System.out.println("1 --- " + 1);
                    return v.getI1() == 0;
                })
                .filter(v -> {
                    System.out.println("2 --- " + 2);
                    return v.getI2() == 0;
                })
                .count();
        System.out.println(count);
    }

    @Test
    public void t2() {
        Stream<Integer> integerStream = Stream.of(1, 2, 3, 4);
        Stream<Integer> integerStream1 = Stream.of(9, 10, 11);
        integerStream = Stream.of(integerStream, integerStream1).flatMap(Function.identity());
        integerStream.forEach(System.out::println);
    }

}
