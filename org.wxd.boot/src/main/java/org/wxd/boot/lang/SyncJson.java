package org.wxd.boot.lang;

import lombok.NoArgsConstructor;
import org.wxd.boot.collection.ObjMap;
import org.wxd.boot.str.json.FastJsonUtil;

/**
 * 用户rpc同步体
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-11-01 16:58
 **/
@NoArgsConstructor
public class SyncJson extends ObjMap {

    public static SyncJson parse(String json) {
        return FastJsonUtil.parse(json, SyncJson.class);
    }

    public static SyncJson build(int code) {
        return new SyncJson().code(code);
    }

    public static SyncJson ok() {
        return new SyncJson().code(1);
    }

    public static SyncJson error(String msg) {
        return error(99, msg);
    }

    /** code不等于1表示异常 */
    public static SyncJson error(int code, String msg) {
        return new SyncJson().code(code).errorMsg(msg);
    }

    /** code不等于1表示异常 */
    public boolean isOk() {
        return getIntValue("code") == 1;
    }

    /** code不等于1表示异常 */
    public boolean isError() {
        return !isOk();
    }

    /** code不等于1表示异常 */
    public int code() {
        return getIntValue("code");
    }

    public SyncJson code(int c) {
        put("code", c);
        return this;
    }

    public String errorMsg() {
        return getString("error");
    }

    public SyncJson errorMsg(String m) {
        put("error", m);
        return this;
    }

    public String data() {
        return getString("data");
    }

    public SyncJson data(Object data) {
        put("data", data);
        return this;
    }

    public <R> R data(Class<R> r) {
        return parseObject("data", r);
    }

    @Override public SyncJson append(Object key, Object value) {
        super.append(key, value);
        return this;
    }
}
