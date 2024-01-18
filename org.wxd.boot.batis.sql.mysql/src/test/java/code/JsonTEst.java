package code;

import com.carrotsearch.hppc.IntIntHashMap;
import lombok.Getter;
import lombok.Setter;
import org.junit.Test;
import org.wxd.boot.batis.sql.SqlEntityTable;
import org.wxd.boot.batis.sql.mysql.MysqlDataWrapper;
import org.wxd.boot.batis.struct.DbColumn;
import org.wxd.boot.str.json.FastJsonUtil;

/**
 * E
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-06-13 19:38
 **/
public class JsonTEst {
    @Test
    public void t0() {
        T object = new T();
        object.t2.a = 4;
        String json = FastJsonUtil.toJson(object);
        System.out.println(json);
        T object1 = FastJsonUtil.parse("{\"a\":1,\"b\":3,\"c\":2,\"t2\":{\"a\":4,\"b\":2,\"c\":2}}", T.class);
        System.out.println(FastJsonUtil.toJson(object1));
        MysqlDataWrapper mysqlDataWrapper = MysqlDataWrapper.Default;
        SqlEntityTable entityTable = mysqlDataWrapper.asEntityTable(T.class);
        System.out.println(entityTable.toDataString());
    }

    @Getter
    @Setter
    public static class T {
        @DbColumn(key = true)
        private int a = 1;
        private final int b = 2;
        private transient int c = 2;

        private final T2 t2 = new T2();
    }

    @Getter
    @Setter
    public static class T2 {
        private int a = 1;
        private final int b = 2;
        private transient int c = 2;
    }

    @Test
    public void intMap() {
        IntIntHashMap intMap = new IntIntHashMap();
        intMap.put(1, 2);
        intMap.put(2, 2);
        String json = FastJsonUtil.toJson(intMap);
        System.out.println(json);
        IntIntHashMap parse = FastJsonUtil.parse(json, IntIntHashMap.class);
        System.out.println(parse);
    }

}
