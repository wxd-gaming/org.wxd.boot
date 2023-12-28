package org.wxd.boot.batis.redis;

import com.alibaba.fastjson.TypeReference;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.agent.function.ConsumerE1;
import org.wxd.boot.agent.function.FunctionE;
import org.wxd.boot.agent.io.FileUtil;
import org.wxd.boot.agent.zip.OutZipFile;
import org.wxd.boot.agent.zip.ReadZipFile;
import org.wxd.boot.append.StreamWriter;
import org.wxd.boot.batis.DataHelper;
import org.wxd.boot.batis.DataWrapper;
import org.wxd.boot.batis.DbConfig;
import org.wxd.boot.batis.EntityTable;
import org.wxd.boot.str.json.FastJsonUtil;
import org.wxd.boot.timer.MyClock;
import redis.clients.jedis.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * redis 单例模式的链接池
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-12-30 20:33
 */
@Slf4j
@Getter
public class RedisDataHelper extends DataHelper<EntityTable, DataWrapper<EntityTable>>
        implements RedisSet, RedisGet, RedisDel, RedisIncr, RedisSort, RedisExist {

    private JedisPoolConfig jedisPoolConfig = null;
    private JedisPool jedisPool = null;

    protected RedisDataHelper() {
    }

    public RedisDataHelper(String ip, int port, String pwd) {
        this(RedisDataWrapper.Default, ip, port, pwd);
    }

    public RedisDataHelper(DbConfig dbConfig) {
        this(RedisDataWrapper.Default, dbConfig);
    }

    public RedisDataHelper(DataWrapper dataWrapper, String ip, int port, String pwd) {
        this(dataWrapper, new DbConfig().setDbHost(ip).setDbPort(port).setDbPwd(pwd));
    }

    public RedisDataHelper(DataWrapper dataWrapper, DbConfig dbConfig) {
        super(dataWrapper, dbConfig);
        log.info("{} 启动 redis db host={} serviceName={}", this.getClass(), dbConfig.getDbHost(), dbConfig.getName());
    }

    public void initDao() {
        jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(30);
        jedisPoolConfig.setMaxIdle(20);
        jedisPoolConfig.setMinIdle(10);
        jedisPoolConfig.setMaxWait(Duration.ofMillis(3000));
        jedisPoolConfig.setTimeBetweenEvictionRuns(Duration.ofMillis(3000));
        jedisPoolConfig.setMinEvictableIdleTime(Duration.ofMillis(3000));
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPoolConfig.setTestOnCreate(true);
        jedisPoolConfig.setTestOnReturn(true);
        jedisPoolConfig.setTestWhileIdle(true);
        this.jedisPool = new JedisPool(
                jedisPoolConfig,
                this.getDbConfig().getDbHost(),
                this.getDbConfig().getDbPort(),
                2000,
                this.getDbConfig().getDbPwd()
        );
    }

    @Override
    public void close() {
        jedisPool.close();
        log.info("{} 关闭 redis db host={} serviceName={}", this.getClass(), dbConfig.getDbHost(), dbConfig.getName());
    }

    /**
     * 获取数据库链接，必须try finally close
     *
     * @param dbIndex redis 数据库插槽
     * @return conn
     */
    @Deprecated
    @SuppressWarnings("all")
    public Jedis getJedis(int dbIndex) {
        if (jedisPool == null) {
            initDao();
        }
        Jedis jedis = jedisPool.getResource();
        jedis.select(dbIndex);
        return jedis;
    }

    /**
     * @param dbIndex
     * @param jedisRFunction
     */
    public <R> R jedis(int dbIndex, FunctionE<Jedis, R> jedisRFunction) {
        try (Jedis jedis = getJedis(dbIndex)) {
            return jedisRFunction.apply(jedis);
        } catch (Exception e) {
            throw Throw.as(e);
        }
    }

    /**
     * @param dbIndex
     * @param consumerJedis
     */
    public void consumer(int dbIndex, ConsumerE1<Jedis> consumerJedis) {
        try (Jedis jedis = getJedis(dbIndex)) {
            consumerJedis.accept(jedis);
        } catch (Exception e) {
            throw Throw.as(e);
        }
    }

    public void consumerPipeline(ConsumerE1<Pipeline> consumerPipeline) {
        consumerPipeline(0, consumerPipeline);
    }

    /**
     * @param dbIndex
     * @param consumerPipeline
     */
    public void consumerPipeline(int dbIndex, ConsumerE1<Pipeline> consumerPipeline) {
        try (Jedis jedis = getJedis(dbIndex)) {
            try (Pipeline pipelined = jedis.pipelined()) {
                consumerPipeline.accept(pipelined);
                pipelined.sync();
            }
        } catch (Exception e) {
            throw Throw.as(e);
        }
    }

    /**
     * @param dbIndex
     * @param pipelineFunctionE
     */
    public <R> R pipeline(int dbIndex, FunctionE<Pipeline, Response<R>> pipelineFunctionE) {
        try (Jedis jedis = getJedis(dbIndex)) {
            try (Pipeline pipelined = jedis.pipelined()) {
                Response<R> ret = pipelineFunctionE.apply(pipelined);
                pipelined.sync();
                return ret.get();
            }
        } catch (Exception e) {
            throw Throw.as(e);
        }
    }

    /**
     * @param dbIndex
     * @param pipelineFunctionE
     */
    public <R> R pipeline0(int dbIndex, FunctionE<Pipeline, R> pipelineFunctionE) {
        try (Jedis jedis = getJedis(dbIndex)) {
            try (Pipeline pipelined = jedis.pipelined()) {
                R ret = pipelineFunctionE.apply(pipelined);
                return ret;
            }
        } catch (Exception e) {
            throw Throw.as(e);
        }
    }

    /** 设置过期时间 这个时间之后过期，单位秒 */
    public void expire(int dbIndex, String redisKey, long seconds) {
        pipeline(dbIndex, pipeline -> pipeline.expire(redisKey, seconds));
    }

    /** 设置过期时间 这个时间之后过期，单位 毫秒 */
    public void pexpire(int dbIndex, String redisKey, long milliseconds) {
        pipeline(dbIndex, pipeline -> pipeline.pexpire(redisKey, milliseconds));
    }

    /** 设置为了时间磋，过期 单位 秒 */
    public void expireAt(int dbIndex, String redisKey, long seconds) {
        pipeline(dbIndex, pipeline -> pipeline.expireAt(redisKey, seconds));
    }

    /** 设置为了时间磋，过期，单位 毫秒 */
    public void pexpireAt(int dbIndex, String redisKey, long milliseconds) {
        pipeline(dbIndex, pipeline -> pipeline.pexpireAt(redisKey, milliseconds));
    }

    /** 关闭链接池 */
    public void destroy() {
        this.jedisPool.destroy();
    }

    public void out2File(String ouDir) {
        out2File(ouDir, 10, "*");
    }

    /**
     * @param ouDir
     * @param maxDbIndex redis db插槽
     * @param pattenKey  清理键
     */
    public void out2File(String ouDir, int maxDbIndex, String pattenKey) {
        StreamWriter streamWriter = new StreamWriter(2048);
        String zipFileName = MyClock.formatDate(MyClock.SDF_YYYYMMDDHHMMSS_4) + ".zip";
        streamWriter.writeLn().write("备份redis：").write(zipFileName).write("\n");
        try (OutZipFile outZipFile = new OutZipFile(ouDir + "/redis/" + zipFileName)) {
            for (int dbIndex = 0; dbIndex < maxDbIndex; dbIndex++) {
                try (Jedis jedis = getJedis(dbIndex)) {
                    Set<String> keys = jedis.keys(pattenKey);
                    for (String key : keys) {
                        String key_type = jedis.type(key);
                        if ("none".equalsIgnoreCase(key_type)) {
                            continue;
                        }
                        outZipFile.newZipEntry(String.valueOf(dbIndex + "/" + key));
                        outZipFile.write(key_type + "\n");
                        long count = 0;
                        if ("string".equalsIgnoreCase(key_type)) {
                            String smember = jedis.get(key);
                            outZipFile.write(smember + "\n");
                            count = 1;
                        } else if ("list".equalsIgnoreCase(key_type)) {
                            count = jedis.llen(key);
                            for (long i = 0; i < count; i++) {
                                String smember = jedis.lindex(key, i);
                                outZipFile.write(smember + "\n");
                            }
                        } else if ("set".equalsIgnoreCase(key_type)) {
                            Set<String> smembers = jedis.smembers(key);
                            for (String smember : smembers) {
                                outZipFile.write(smember + "\n");
                            }
                            count = smembers.size();
                        } else if ("zset".equalsIgnoreCase(key_type)) {
                            /*有序集合*/
                            List<String> zrange = jedis.zrange(key, 0, -1);
                            Map<String, Double> jsonObject = new HashMap<>();
                            for (String smember : zrange) {
                                jsonObject.clear();
                                jsonObject.put(smember, jedis.zscore(key, smember));
                                outZipFile.write(FastJsonUtil.toJson(jsonObject) + "\n");
                            }
                            count = zrange.size();
                        } else if ("hash".equalsIgnoreCase(key_type)) {
                            Map<String, String> hgetAll = jedis.hgetAll(key);
                            /*有序集合*/
                            Map<String, String> jsonObject = new HashMap<>();
                            for (Map.Entry<String, String> entry : hgetAll.entrySet()) {
                                jsonObject.clear();
                                jsonObject.put(entry.getKey(), entry.getValue());
                                outZipFile.write(FastJsonUtil.toJson(jsonObject) + "\n");
                            }
                            count = hgetAll.size();
                        }
                        streamWriter.writeLn("数据插槽：" + dbIndex + ", key：" + key + ", type：" + key_type + ", 行：" + count);
                    }
                }
            }
        }
        log.warn(streamWriter.toString());
    }

    public void inDb4File(String zipFile, int batchSize) {
        try (ReadZipFile readZipFile = new ReadZipFile(zipFile)) {
            readZipFile.forEach(
                    (tableName, bytes) -> {
                        String s = new String(bytes, StandardCharsets.UTF_8);

                        try (StringReader stringReader = new StringReader(s)) {
                            try (BufferedReader bufferedReader = new BufferedReader(stringReader)) {

                                AtomicLong size = new AtomicLong();

                                File file = new File(tableName);
                                int dbIndex = Integer.valueOf(file.getParent());
                                String key = file.getName();
                                String key_type = bufferedReader.readLine();
                                consumerPipeline(dbIndex, pipeline -> {

                                    if ("string".equalsIgnoreCase(key_type)) {
                                        pipeline.set(key, bufferedReader.readLine());
                                        size.incrementAndGet();
                                    } else if ("list".equalsIgnoreCase(key_type)) {
                                        String line;
                                        while ((line = bufferedReader.readLine()) != null) {
                                            pipeline.rpush(key, line);
                                            size.incrementAndGet();
                                        }
                                    } else if ("set".equalsIgnoreCase(key_type)) {
                                        String line;
                                        while ((line = bufferedReader.readLine()) != null) {
                                            pipeline.sadd(key, line);
                                            size.incrementAndGet();
                                        }
                                    } else if ("zset".equalsIgnoreCase(key_type)) {
                                        /*有序集合*/
                                        String line;
                                        while ((line = bufferedReader.readLine()) != null) {
                                            Map<String, Double> jsonObject = FastJsonUtil.parse(line, new TypeReference<HashMap<String, Double>>() {
                                            });
                                            for (Map.Entry<String, Double> entry : jsonObject.entrySet()) {
                                                pipeline.zadd(key, entry.getValue(), entry.getKey());
                                            }
                                            size.addAndGet(jsonObject.size());
                                        }
                                    } else if ("hash".equalsIgnoreCase(key_type)) {
                                        String line;
                                        while ((line = bufferedReader.readLine()) != null) {
                                            Map<String, String> jsonObject = FastJsonUtil.parseStringMap(line);
                                            pipeline.hmset(key, jsonObject);
                                            size.addAndGet(jsonObject.size());
                                        }
                                    }
                                });
                                log.warn("从文件：" + zipFile + ", 数据插槽：" + dbIndex + ", 键：" + key + ", 数据：" + size.get() + " 完成");
                            }
                        }
                    }
            );
        }
        log.info("所有数据 导入 完成：" + FileUtil.getCanonicalPath(zipFile));
    }

}
