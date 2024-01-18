package org.wxd.boot.field.extend;

import java.lang.annotation.*;

/**
 * 属性注解，标注类型匹配
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-12-30 20:33
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({
        ElementType.FIELD,
        ElementType.LOCAL_VARIABLE
})
@interface FieldAnns {

    FieldAnn[] value();
}

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.LOCAL_VARIABLE
})
@Repeatable(FieldAnns.class)
public @interface FieldAnn {
    /** 忽律字段 */
    boolean alligator() default false;

    FieldType[] fieldTypes() default {};

    /** 备注描述 */
    String remarks() default "";

}
