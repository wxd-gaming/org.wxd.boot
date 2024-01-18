package org.wxd.boot.batis.struct;

import java.lang.annotation.*;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-12-30 20:33
 */
@Documented
@Target(
        {
                ElementType.TYPE, /*类*/
                ElementType.LOCAL_VARIABLE/*局部变量*/
        }
)
@Retention(RetentionPolicy.RUNTIME)
public @interface DbTable {

    /** 映射表 默认否 */
    public boolean mappedSuperclass() default false;

    /** 映射名字 */
    public String name() default "";

    /** 大于1表示需要拆分表 */
    public int splitTable() default 0;

    /**
     * 备注
     *
     * @return
     */
    public String comment() default "";

    /**
     * 忽略表
     *
     * @return
     */
    public boolean alligator() default false;
}
