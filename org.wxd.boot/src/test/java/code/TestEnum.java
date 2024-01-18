package code;

import org.wxd.boot.collection.OfMap;
import org.wxd.boot.lang.IEnum;

import java.util.Map;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-04-24 10:21
 **/
public enum TestEnum implements IEnum {
    None(0, "默认值"),
    First(1, "第一名"),
    ;

    private static final Map<Integer, TestEnum> static_map = OfMap.asMap(TestEnum::getCode, TestEnum.values());

    public static TestEnum as(int value) {
        return static_map.get(value);
    }

    private final int code;
    private final String comment;

    TestEnum(int code, String comment) {
        this.code = code;
        this.comment = comment;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getComment() {
        return comment;
    }
}
