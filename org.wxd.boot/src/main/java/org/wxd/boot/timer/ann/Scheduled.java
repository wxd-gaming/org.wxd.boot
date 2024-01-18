package org.wxd.boot.timer.ann;


import java.lang.annotation.*;

/**
 * 定时器任务
 * <p> 默认是 一秒一次
 */
@Documented
@Target({ElementType.METHOD, /*方法*/})
@Retention(RetentionPolicy.RUNTIME)
public @interface Scheduled {

    String name() default "";

    /** 上一次执行尚未完成是否持续执行 默认false 不执行 */
    boolean scheduleAtFixedRate() default false;

    /**
     * 秒 分 时 日 月 星期 年
     * <p> {@code * * * * * * * }
     * <p> 下面以 秒 配置举例
     * <p> * 或者 ? 无限制,
     * <p> 数字是 指定秒执行
     * <p> 0-5 第 0 秒 到 第 5 秒执行 每秒执行
     * <p> 0,5 第 0 秒 和 第 5 秒 各执行一次
     * <p> {@code *}/5 秒 % 5 == 0 执行
     * <p> 5/5 第五秒之后 每5秒执行一次
     * <p> 秒 0-59
     * <p> 分 0-59
     * <p> 时 0-23
     * <p> 日 1-28 or 29 or 30 or 31
     * <p> 月 1-12
     * <p> 星期 1-7 Mon Tues Wed Thur Fri Sat Sun
     * <p> 年 1970 - 2199
     */
    String value() default "";

}
