package org.wxd.boot.lang;

import org.openjdk.jol.info.GraphLayout;
import org.wxd.boot.format.data.Data2Json;
import org.wxd.boot.format.data.Data2StringMap;
import org.wxd.boot.format.data.Data2Xml;
import org.wxd.boot.format.data.DataSerialize;

/**
 * 实现一些序列号接口，重写 hashcode
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-09-12 15:39
 **/
public abstract class ObjectBase
        implements
        Data2Json,
        Data2StringMap,
        DataSerialize,
        Data2Xml {

    public ObjectBase() {
    }

    /** 内存大小,很消耗内存，谨慎使用 */
    public long totalSize() {
        return GraphLayout.parseInstance(this).totalSize();
    }

    @Override
    public String toString() {
        return toJson();
    }

}
