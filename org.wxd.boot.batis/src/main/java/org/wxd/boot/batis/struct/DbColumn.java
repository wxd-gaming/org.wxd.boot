package org.wxd.boot.batis.struct;


import org.wxd.boot.batis.enums.ColumnType;
import org.wxd.boot.batis.enums.SortType;

import java.lang.annotation.*;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-12-30 20:33
 */
@Documented
@Target({ElementType.FIELD/*字段*/})
@Retention(RetentionPolicy.RUNTIME)
public @interface DbColumn {

    /**
     * true 忽略字段
     */
    boolean alligator() default false;

    /**
     * 数据库映射名字
     */
    String name() default "";

    /**
     * 数据类型
     */
    ColumnType columnType() default ColumnType.None;

    /**
     * 主键列
     */
    boolean key() default false;

    /**
     * 默认值
     */
    String defaultValue() default "null";

    /**
     * 字段长度
     */
    int length() default 0;

    /**
     * 普通索引，唯一索引是表级别的锁
     */
    boolean index() default false;

    /**
     * mysql 索引类型 {@code USING HASH} or {@code USING BTREE}
     *
     * @return
     */
    String mysqlIndexType() default "";

    /**
     * true 表示可以为空
     */
    boolean nullable() default true;

    /**
     * 获取数据排序，只有主键列设置有效
     */
    SortType sort() default SortType.ASC;

    /**
     * 字段描述
     */
    String comment() default "";

}
