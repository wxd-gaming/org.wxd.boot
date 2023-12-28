package org.wxd.boot.batis.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-05-17 12:08
 **/
interface RedisGet {

    default String get(RedisKey redisKey, Object... redisParams) {
        return get(redisKey.getDbIndex(), redisKey.redisKey(redisParams));
    }

    default String get(String redisKey) {
        return get(0, redisKey);
    }

    default String get(int dbIndex, String redisKey) {
        try (Jedis jedis = ((RedisDataHelper) this).getJedis(dbIndex)) {
            return jedis.get(redisKey);
        }
    }

    default <R> R hget(RedisKey redisKey, Object field, Function<String, R> function, Object... redisParams) {
        return hget(redisKey.getDbIndex(), redisKey.redisKey(redisParams), field, function);
    }

    /**
     * @param dbIndex
     * @param redisKey
     * @param field
     * @param function 把数据转化为适用的类型
     * @param <R>
     * @return
     */
    default <R> R hget(int dbIndex, String redisKey, Object field, Function<String, R> function) {
        String hget = hget(dbIndex, redisKey, String.valueOf(field));
        if (hget == null) {
            return null;
        }
        return function.apply(hget);
    }

    default String hget(RedisKey redisKey, Object field, Object... redisParams) {
        return hget(redisKey.getDbIndex(), redisKey.redisKey(redisParams), field);
    }

    default String hget(String redisKey, Object field) {
        return hget(0, redisKey, field);
    }

    default String hget(int dbIndex, String redisKey, Object field) {
        try (Jedis jedis = ((RedisDataHelper) this).getJedis(dbIndex)) {
            return jedis.hget(redisKey, String.valueOf(field));
        }
    }


    default Map<String, String> hgetAll(RedisKey redisKey, Object... redisParams) {
        return hgetAll(redisKey.getDbIndex(), redisKey.redisKey(redisParams));
    }

    default Map<String, String> hgetAll(String redisKey) {
        return hgetAll(0, redisKey);
    }

    default Map<String, String> hgetAll(int dbIndex, String redisKey) {
        try (Jedis jedis = ((RedisDataHelper) this).getJedis(dbIndex)) {
            return jedis.hgetAll(redisKey);
        }
    }

    default Map<String, String> hmget(String redisKey, Object... fields) {
        return hmget(0, redisKey, fields);
    }

    default Map<String, String> hmget(int dbIndex, String redisKey, Object... fields) {
        try (Jedis jedis = ((RedisDataHelper) this).getJedis(dbIndex)) {
            try (Pipeline pipelined = jedis.pipelined()) {

                Map<String, Response<String>> responseMap = new LinkedHashMap<>();

                for (Object field : fields) {
                    String valueOf = String.valueOf(field);
                    Response<String> hget = pipelined.hget(redisKey, valueOf);
                    responseMap.put(valueOf, hget);
                }

                pipelined.sync();

                Map<String, String> retMap = new LinkedHashMap<>();
                for (Map.Entry<String, Response<String>> entry : responseMap.entrySet()) {
                    retMap.put(entry.getKey(), entry.getValue().get());
                }
                return retMap;
            }
        }
    }

}
