package org.wxd.boot.starter.batis;

import org.wxd.boot.batis.DbConfig;
import org.wxd.boot.batis.redis.RedisDataHelper;

/**
 * redis
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-12-11 18:19
 **/
public class RedisService extends RedisDataHelper {

    public RedisService(DbConfig dbConfig) {
        super(dbConfig);
    }

}
