package org.wxd.boot.threading;

import java.lang.annotation.*;

/**
 * 指定运行的线程池
 */
@Inherited
@Documented
@Target({
        ElementType.METHOD, /*方法*/
        ElementType.LOCAL_VARIABLE/*局部变量*/
})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExecutorLog {

    /** debug模式下不显示日志，比如心跳太多 */
    boolean showLog() default false;

    /** 执行日志记录时间 */
    long logTime() default 33;

    /** 执行报警时间 */
    long warningTime() default 1000;

}
