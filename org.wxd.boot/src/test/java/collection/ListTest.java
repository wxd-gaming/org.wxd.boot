package collection;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.wxd.boot.collection.ObjIntMap;
import org.wxd.boot.collection.Table;
import org.wxd.boot.collection.concurrent.ConcurrentList;
import org.wxd.boot.collection.concurrent.ConcurrentTable;
import org.wxd.boot.format.data.Data2Json;
import org.wxd.boot.str.json.FastJsonUtil;
import org.wxd.boot.str.json.ParameterizedTypeImpl;
import org.wxd.boot.system.MarkTimer;

import java.io.Serializable;
import java.util.ListIterator;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-05-27 11:39
 **/
@Getter
@Setter
@Slf4j
public class ListTest implements Serializable, Data2Json {

    @Test
    public void t0() {
        final MarkTimer markTimer = MarkTimer.build();
        ConcurrentList<Integer> list = new ConcurrentList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.add(5);
        System.out.println(list);
        final ListIterator<Integer> iterator = list.listIterator();
        while (iterator.hasNext()) {
            Integer next = iterator.next();
            if (next == 4) {
                iterator.remove();
                iterator.add(7);
            }
        }
        System.out.println(list);
        list.sort(null);
        final String toJson = FastJsonUtil.toJson(list);
        System.out.println(toJson);
        final Object parse = FastJsonUtil.parse(toJson, ParameterizedTypeImpl.genericTypes(ConcurrentList.class, ConcurrentList.class, Integer.class));
        System.out.println(parse);
        markTimer.print(log, 500, "ddd");
    }

    @Test
    public void testTupleMap() {

        table.put("1", "2", "5");
        table.put("1", "2", "6");
        table.put("1", "3", "6");
        table.put("2", "2", "5");
        table.put("2", "2", "6");
        table.put("2", "3", "6");

        concurrentTable.put("1", "2", "5");
        concurrentTable.put("1", "2", "6");
        concurrentTable.put("1", "3", "6");
        concurrentTable.put("2", "2", "5");
        concurrentTable.put("2", "2", "6");
        concurrentTable.put("2", "3", "6");


        intMap.put("1", 1);
        intMap.put("2", 2);

        String x = this.toJsonWriteType();
        System.out.println(x);
        ListTest parse = FastJsonUtil.parse(x, this.getClass());
        System.out.println(parse.toJsonWriteType());
    }


    ObjIntMap<String> intMap = new ObjIntMap<>();
    Table<String, String, String> table = new Table<>();
    ConcurrentTable<String, String, String> concurrentTable = new ConcurrentTable<>();
}
