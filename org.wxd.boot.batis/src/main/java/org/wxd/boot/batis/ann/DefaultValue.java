package org.wxd.boot.batis.ann;

import java.lang.annotation.*;

/**
 * 用于排序的注解
 */
@Documented
@Target({ElementType.FIELD/*字段*/})
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultValue {

    /** 默认 99999 */
    String value() default "";

}

