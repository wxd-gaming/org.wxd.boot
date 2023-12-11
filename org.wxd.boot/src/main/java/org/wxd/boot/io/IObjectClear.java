package org.wxd.boot.io;

/**
 * 对象池的清理
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-08-14 12:28
 **/
public interface IObjectClear {

    void clear();

    default void returnObject() {
        ObjectFactory.returnObject(this);
    }

}
