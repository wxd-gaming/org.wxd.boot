package org.wxd.boot.collection;

import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 各种转化
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-11-03 17:24
 **/
public class OfSet implements Serializable {

    public static Set empty() {
        return Collections.EMPTY_SET;
    }

    public static Set<Integer> asSet(int[] args) {
        Set<Integer> set = new LinkedHashSet<>(args.length + 1);
        return asSet(set, args);
    }

    public static Set<Integer> asSet(Set<Integer> set, int[] args) {
        for (int t : args) {
            set.add(t);
        }
        return set;
    }

    public static Set<Long> asSet(long[] args) {
        Set<Long> set = new LinkedHashSet<>(args.length + 1);
        return asSet(set, args);
    }

    public static Set<Long> asSet(Set<Long> set, long[] args) {
        for (long t : args) {
            set.add(t);
        }
        return set;
    }

    public static <T> Set<T> asSet(T... args) {
        Set<T> list = new LinkedHashSet<>(args.length + 1);
        return asSet(list, args);
    }

    public static <T> Set<T> asSet(Set<T> list, T... args) {
        for (T t : args) {
            list.add(t);
        }
        return list;
    }

    /** 构建双重 HashSet */
    public static <T> Set<Set<T>> asSets(T[]... args) {
        Set<Set<T>> sets = new LinkedHashSet<>(args.length + 1);
        return asSets(sets, args);
    }

    public static <T> Set<Set<T>> asSets(Set<Set<T>> sets, T[]... args) {
        for (T[] ts : args) {
            final Set<T> row = asSet(ts);
            sets.add(row);
        }
        return sets;
    }
}
