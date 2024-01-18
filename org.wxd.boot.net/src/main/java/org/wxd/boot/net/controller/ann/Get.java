package org.wxd.boot.net.controller.ann;

import java.lang.annotation.*;

@Documented
@Target({
        ElementType.METHOD, /*方法*/
        ElementType.LOCAL_VARIABLE/*局部变量*/
})
@Retention(RetentionPolicy.RUNTIME)
@TextMapping
public @interface Get {
}
