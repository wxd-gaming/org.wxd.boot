package code;

import org.junit.Test;
import org.wxd.boot.format.UniqueID;
import org.wxd.boot.str.json.FastJsonUtil;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * id生成测试
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-11-23 17:18
 **/
public class UidTest {

    @Test
    public void t1() {

        UniqueID.Uid uid = new UniqueID.Uid(new AtomicLong(2));
        String json = uid.toJson();
        System.out.println(json);
        UniqueID.Uid parse = FastJsonUtil.parse(json, UniqueID.Uid.class);
        System.out.println(parse.toJson());

        HashSet<Long> longs = new HashSet<>();
        UniqueID.TimeUid idFormat = new UniqueID.TimeUid();

        while (longs.add(idFormat.next(1))) {}
        System.out.println(longs.size());
    }

}
