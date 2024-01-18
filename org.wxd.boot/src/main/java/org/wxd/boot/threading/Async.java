package org.wxd.boot.threading;

import java.lang.annotation.*;

/**
 * 指定运行的线程池
 */
@Documented
@Target({ElementType.METHOD/*方法*/})
@Retention(RetentionPolicy.RUNTIME)
public @interface Async {

    /** 标记是否虚拟线程 指定thread 后失效 */
    boolean vt() default false;

    String threadName() default "";

    /** 执行队列名称 */
    String queueName() default "";

}
