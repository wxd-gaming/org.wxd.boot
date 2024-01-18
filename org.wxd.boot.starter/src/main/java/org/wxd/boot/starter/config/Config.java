package org.wxd.boot.starter.config;


import java.lang.annotation.*;

/**
 * 配置类注解
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-04-15 08:50
 **/
@Target({ElementType.TYPE/*类*/})
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Config {

    /** 注明配置文件位置和名字 */
    String value() default "";

    /** 配置类型，默认json */
    ConfigType configType() default ConfigType.Json;

    /** 当没有找到配置文件是否初始化 */
    boolean notConfigInit() default false;

    /** true 表示如果么有配置文件就忽律 */
    boolean notConfigAlligator() default false;
}
