package org.wxd.boot.batis.ann;

import java.lang.annotation.*;

@Inherited
@Documented
@Target({ElementType.TYPE/*类*/})
@Retention(RetentionPolicy.RUNTIME)
public @interface DbBean {
}
