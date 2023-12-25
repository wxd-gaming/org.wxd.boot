package org.wxd.boot.agent.function;

/**
 * 创建
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-12-25 15:11
 **/
public interface Factory3<T1, T2, T3, R> {

    /**
     * Gets a result.
     *
     * @return a result
     */
    R get(T1 t1, T2 t2, T3 t3) throws Exception;
}
