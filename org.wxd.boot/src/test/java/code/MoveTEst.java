package code;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.wxd.boot.struct.MoveUtil;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-06-13 12:05
 **/
@Slf4j
public class MoveTEst {

    @Test
    public void t12() {
        System.out.println(MoveUtil.distance(1, 10, 5, 15));
        System.out.println(distance(1, 10, 5, 15));
    }

    public int distance(int x1, int y1, int x2, int y2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }

}
