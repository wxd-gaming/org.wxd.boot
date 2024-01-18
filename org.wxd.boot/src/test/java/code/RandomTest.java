package code;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.Serializable;
import java.util.Random;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-04-27 15:45
 **/
@Slf4j
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
public class RandomTest implements Serializable {

    @Test
    public void next() {
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            System.out.println(random(random, i, 100, 300));
        }
    }

    public int random(Random random, int seed, int min, int max) {
        return new Random(seed).nextInt(max - min) + min;
    }

}
