package org.wxd.boot.ann;

import java.lang.annotation.*;

/**
 * 用于排序的注解
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD/*字段*/})
@Retention(RetentionPolicy.RUNTIME)
public @interface Sort {
    /** 分组 默认空 */
    String group() default "";

    /** 默认 99999 */
    int value() default 99999;

}

