package org.wxd.boot.collection;

import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.append.StreamWriter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 各种转化
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-11-03 17:24
 **/
public class OfList implements Serializable {

    public static List empty() {
        return Collections.emptyList();
    }

    public static String asString(CharSequence delimiter, Collection list) {
        try (StreamWriter streamWriter = new StreamWriter()) {
            for (Object o : list) {
                if (!streamWriter.isEmpty()) streamWriter.write(delimiter);
                streamWriter.write(o);
            }
            return streamWriter.toString();
        }
    }

    public static List<Integer> asList(int[] args) {
        List<Integer> list = new ArrayList<>(args.length + 1);
        return asList(list, args);
    }

    public static List<Integer> asList(List<Integer> list, int[] args) {
        for (int t : args) {
            list.add(t);
        }
        return list;
    }

    public static List<Long> asList(long[] args) {
        List<Long> list = new ArrayList<>(args.length + 1);
        return asList(list, args);
    }

    public static List<Long> asList(List<Long> list, long[] args) {
        for (long t : args) {
            list.add(t);
        }
        return list;
    }

    public static <T> List<T> asList(T... args) {
        List<T> list = new ArrayList<>(args.length + 1);
        return asList(list, args);
    }

    public static <T> List<T> asList(List<T> list, T... args) {
        for (T t : args) {
            list.add(t);
        }
        return list;
    }

    /** 构建双重list */
    public static <T> List<List<T>> asLists(T[]... args) {
        List<List<T>> list = new ArrayList<>(args.length + 1);
        for (T[] ts : args) {
            final List<T> row = asList(ts);
            list.add(row);
        }
        return list;
    }

}
