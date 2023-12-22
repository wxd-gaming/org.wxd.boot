package org.wxd.boot.net.controller.ann;

import java.lang.annotation.*;

/**
 * 注意，使用这个注解方法的时候产生要求
 */
@Documented
@Target({
        ElementType.METHOD, /*方法*/
        ElementType.LOCAL_VARIABLE/*局部变量*/
})
@Retention(RetentionPolicy.RUNTIME)
public @interface TextMapping {

    /** url 会覆盖 {@link TextController}.url() */
    String url() default "";

    /** 路由名称 */
    String mapping() default "";

    /** 备注 */
    String remarks() default "";

    /** 需要的权限 */
    int needAuth() default 0;

    /** 权限不足提示 */
    String authTips() default "权限认证失败";

}
