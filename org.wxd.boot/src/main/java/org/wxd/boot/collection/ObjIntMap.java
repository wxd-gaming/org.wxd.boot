package org.wxd.boot.collection;


import org.wxd.boot.lang.ObjectBase;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

/** 线程安全的 */
public class ObjIntMap<K extends Comparable<K>> extends ObjectBase implements Map<K, Integer> {
    private final ConcurrentSkipListMap<K, Integer> map = new ConcurrentSkipListMap<>();

    public int getCount(K key) {
        return map.getOrDefault(key, 0);
    }

    public int putCount(K key, int newValue) {
        return getAndUpdate(key, x -> newValue);
    }

    public int sum() {
        return map.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int incrementAndGet(K key) {
        return addAndGet(key, 1);
    }

    public int decrementAndGet(K key) {
        return addAndGet(key, -1);
    }

    public int addAndGet(K key, int delta) {
        return accumulateAndGet(key, delta, Integer::sum);
    }

    public int getAndIncrement(K key) {
        return getAndAdd(key, 1);
    }

    public int getAndDecrement(K key) {
        return getAndAdd(key, -1);
    }

    public int getAndAdd(K key, int delta) {
        return getAndAccumulate(key, delta, Integer::sum);
    }

    /** 更新新数据 */
    public int updateAndGet(K key, IntUnaryOperator updaterFunction) {
        return map.compute(key, (k, value) -> updaterFunction.applyAsInt((value == null) ? 0 : value));
    }

    /** 返回老数据，更新新数据 */
    private int getAndUpdate(K key, IntUnaryOperator updaterFunction) {
        AtomicInteger holder = new AtomicInteger();
        map.compute(
                key,
                (k, value) -> {
                    int oldValue = (value == null) ? 0 : value;
                    holder.set(oldValue);
                    return updaterFunction.applyAsInt(oldValue);
                });
        return holder.get();
    }

    private int accumulateAndGet(K key, int x, IntBinaryOperator accumulatorFunction) {
        return updateAndGet(key, oldValue -> accumulatorFunction.applyAsInt(oldValue, x));
    }

    private int getAndAccumulate(K key, int x, IntBinaryOperator accumulatorFunction) {
        return getAndUpdate(key, oldValue -> accumulatorFunction.applyAsInt(oldValue, x));
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Deprecated
    @Override
    public Integer get(Object key) {
        return map.get(key);
    }

    @Deprecated
    @Override
    public Integer put(K key, Integer value) {
        return map.put(key, value);
    }

    @Override
    public Integer remove(Object key) {
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends Integer> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<Integer> values() {
        return map.values();
    }

    @Override
    public Set<Entry<K, Integer>> entrySet() {
        return map.entrySet();
    }
}
