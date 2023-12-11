package org.wxd.boot.batis.redis;


import org.wxd.boot.batis.DataWrapper;
import org.wxd.boot.batis.EntityTable;

import java.io.Serializable;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-01-21 10:15
 **/
public class RedisDataWrapper extends DataWrapper<EntityTable> implements Serializable {

    public static RedisDataWrapper Default = new RedisDataWrapper();

    @Override
    public EntityTable createEntityTable() {
        return new EntityTable();
    }

}
