package code;

import org.junit.Test;
import org.wxd.boot.lang.rank.RankMap;
import org.wxd.boot.lang.rank.RankScore;

/**
 * 排行榜测试
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-11-27 11:51
 **/
public class RankTest {

    @Test
    public void stream() {
        RankMap<Long, RankScore<Long>> rankMap = new RankMap<>();
        rankMap.addScore(1L, 1);
        rankMap.addScore(1L, 2);
        rankMap.addScore(2L, 1);
        rankMap.addScore(2L, 2);
        rankMap.addScore(3L, 2);
        rankMap.getRange(RankScore.Sort, 0, 20).forEach(System.out::println);
        System.out.println("===================================");
        rankMap.getRange(RankScore.BreSort, 0, 20).forEach(System.out::println);
    }

}
