package org.wxd.boot.system;

import java.io.Serializable;

/**
 * 计算辅助工具
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-03-08 09:49
 **/
public class MathUtil implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 获取一个算法，把数据除以一个数得到他的商
     * {@code source / chu + (source % chu>0?:1:0)}
     *
     * @param source
     * @param chu
     * @return
     */
    public static int divisor(int source, int chu) {
        int i = source / chu;
        final int i2 = source % chu;
        if (i2 > 0) {
            i++;
        }
        return i;
    }

    /**
     * 获取一个算法，把数据除以一个数得到他的商
     * {@code source / chu + (source % chu>0?:1:0)}
     *
     * @param source
     * @param chu
     * @return
     */
    public static long divisor(long source, long chu) {
        long i = source / chu;
        final long i2 = source % chu;
        if (i2 > 0) {
            i++;
        }
        return i;
    }

}
