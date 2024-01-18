package org.wxd.boot.cache;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.function.ConsumerE2;
import org.wxd.boot.collection.concurrent.ConcurrentList;
import org.wxd.boot.threading.Event;
import org.wxd.boot.threading.Executors;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 缓存 块
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-11-22 09:24
 **/
@Slf4j
@Getter
@Setter
@Accessors(chain = true)
public class CachePack<K, V> implements Serializable {

    private static final ConcurrentList<CachePack> CACHE_PACKS = new ConcurrentList<>();

    static {
        Runnable command = new Event("缓存定时处理", 2000, 20000) {

            @Override public void onEvent() {
                long currentTimeMillis = System.currentTimeMillis();
                Iterator<CachePack> cachePackIterator = CACHE_PACKS.iterator();

                while (cachePackIterator.hasNext()) {

                    CachePack cachePack = cachePackIterator.next();

                    if (currentTimeMillis - cachePack.lastCacheIntervalTime < cachePack.cacheIntervalTime) {
                        /*减少消耗*/
                        continue;
                    }

                    cachePack.lastCacheIntervalTime = currentTimeMillis;

                    Iterator<Map.Entry<Object, CacheValue>> iterator = cachePack.cacheValues.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<Object, CacheValue> next = iterator.next();
                        Object key = next.getKey();
                        CacheValue cacheValue = next.getValue();
                        try {
                            if (cacheValue.getValue() != null) {

                                Boolean apply = true;

                                if (cachePack.getHeart() != null) {
                                    if (cachePack.getCacheHeartTimer() > 0) {
                                        if (cacheValue.getLastHeartTime() == 0) {
                                            cacheValue.setLastHeartTime(currentTimeMillis);
                                        }
                                        if (currentTimeMillis - cacheValue.getLastHeartTime() > cachePack.getCacheHeartTimer()) {
                                            try {
                                                apply = (Boolean) cachePack.getHeart().apply(cacheValue.getValue());
                                            } catch (Throwable throwable) {
                                                log.warn(cachePack.getCacheName() + ", 定时检查数据异常：", throwable);
                                            }
                                            cacheValue.setLastHeartTime(currentTimeMillis);
                                        }
                                    }
                                }

                                if (apply != null && !apply) continue;/*通过心跳处理返回不可清理*/

                                if (cacheValue.getSurvivalTime() < 0) {
                                    /*无需清理的集合*/
                                    continue;
                                }

                                /*滑动缓存清理*/
                                if (cacheValue.isSlideCache()
                                        && currentTimeMillis - cacheValue.getLastGetCacheTime() < cacheValue.getSurvivalTime()) {
                                    continue;
                                }

                                /*固定缓存清理*/
                                if (!cacheValue.isSlideCache()
                                        && currentTimeMillis - cacheValue.getCreateTime() < cacheValue.getSurvivalTime()) {
                                    continue;
                                }
                            }

                            iterator.remove();
                            cachePack.remove(key, cacheValue.getValue(), "定时移除");
                        } catch (Throwable throwable) {
                            log.warn(cachePack.getCacheName() + ", 定时检查数据异常：", throwable);
                        }
                    }
                }
            }
        };

        Executors.getDefaultExecutor().scheduleAtFixedDelay(command, 10, 10, TimeUnit.MILLISECONDS);
    }

    private String cacheName;
    /** cache的心跳执行, 单位毫秒 */
    protected volatile long cacheIntervalTime = 0;
    /** 最后一次执行心跳时间, 单位毫秒 */
    protected volatile long lastCacheIntervalTime = 0;
    /** 默认缓存策略, true 滑动缓存 */
    protected volatile boolean cacheSlide = true;
    /** 默认清理时间 , 单位毫秒 */
    protected volatile long cacheSurvivalTime = -1;
    /** 执行心跳处理 , 单位毫秒 */
    protected volatile long cacheHeartTimer = -1;
    /** 缓存加载 */
    public Function<K, V> loading;
    /** 定时心跳 返回 true 才能允许删除，否者即便上过期也不能删除 */
    public Function<V, Boolean> heart;
    /** 缓存卸载 */
    public ConsumerE2<V, String> unload;

    private final ConcurrentHashMap<K, CacheValue<V>> cacheValues = new ConcurrentHashMap<>();

    public CachePack() {
        CACHE_PACKS.add(this);
    }

    public int cacheSize() {
        return this.cacheValues.size();
    }

    /**
     * 所有的key
     *
     * @return
     */
    public List<K> keys() {
        return new LinkedList<>(this.cacheValues.keySet());
    }

    /**
     * 拷贝一份缓存对象
     *
     * @return
     */
    public Map<K, V> allCache() {
        LinkedHashMap<K, V> linkedHashMap = new LinkedHashMap<>();
        for (Map.Entry<K, CacheValue<V>> entry : this.cacheValues.entrySet()) {
            linkedHashMap.put(entry.getKey(), entry.getValue().getValue());
        }
        return linkedHashMap;
    }

    public void forEach(ConsumerE2<K, V> each) throws Exception {
        for (Map.Entry<K, CacheValue<V>> entry : cacheValues.entrySet()) {
            each.accept(entry.getKey(), entry.getValue().getValue());
        }
    }

    public V cache(K k) {
        return cache(k, this.getLoading() != null);
    }

    public V cache(K k, boolean load) {
        if (k == null) {
            return null;
        }
        CacheValue<V> cacheValue = this.cacheValues.computeIfAbsent(k, l -> {
            if (load && this.getLoading() != null) {
                V apply = this.getLoading().apply(l);
                if (apply != null) {
                    return new CacheValue<V>()
                            .setSlideCache(this.cacheSlide)
                            .setSurvivalTime(this.cacheSurvivalTime)
                            .setValue(apply);
                }
            }
            return null;
        });
        if (cacheValue == null) return null;
        /*更新最后获取缓存的时间*/
        cacheValue.setLastGetCacheTime(System.currentTimeMillis());
        return cacheValue.getValue();
    }

    public void addCache(K k, V v) {
        addCache(k, v, false, this.cacheSlide, this.cacheSurvivalTime);
    }

    /**
     * 有效期
     *
     * @param k
     * @param v
     * @param survivalTime 过期时间
     */
    public void addCache(K k, V v, long survivalTime) {
        addCache(k, v, false, this.cacheSlide, survivalTime);
    }

    public void addCache(K k, V v, boolean isUpdate, boolean slide, long survivalTime) {
        CacheValue<V> cacheValue = this.cacheValues.get(k);
        if (!isUpdate && cacheValue != null) {
            throw new RuntimeException(this.cacheName + " 存在的相同的缓存：" + k);
        }
        if (cacheValue == null) {
            cacheValue = new CacheValue<V>()
                    .setSlideCache(slide)
                    .setSurvivalTime(survivalTime)
                    .setValue(v);
        } else {
            cacheValue.setValue(v);
        }
        this.cacheValues.put(k, cacheValue);
    }

    public void clear() {
        final List<K> keys = keys();
        for (K key : keys) {
            remove(key);
        }
    }

    public void remove(K k) {
        if (k == null) {
            return;
        }
        CacheValue<V> cacheValue = this.cacheValues.remove(k);
        if (cacheValue != null) {
            this.remove(k, cacheValue.getValue(), "主动移除");
        }
    }

    private void remove(K k, V v, String logs) {
        if (this.getUnload() == null) {
            log.info(logs + " -> " + this.cacheName + ", key=" + k + ", value=" + v);
            return;
        }
        try {
            this.getUnload().accept(v, logs);
        } catch (Exception ex) {
            log.error(logs + " -> " + this.cacheName + ", key=" + k + ", 移除缓存异常", ex);
        }

    }

    @Override
    public String toString() {
        return "{" +
                "cacheName='" + cacheName + '\'' +
                ", cacheIntervalTime='" + cacheIntervalTime + '\'' +
                ", cacheSurvivalTime='" + cacheSurvivalTime + '\'' +
                ", size='" + cacheValues.size() + '\'' +
                '}';
    }
}
