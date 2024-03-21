package wxdgaming.boot.net.controller;

import wxdgaming.boot.net.controller.ann.TextMapping;

import java.lang.reflect.Method;

/**
 * cmd执行映射
 *
 * @param path     路由
 * @param instance 实例
 * @param method   方法
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-12-12 20:13
 */
public record TextMappingRecord(TextMapping textMapping,
        String path, String remarks, MappingFactory.TextMappingProxy mapping, Object instance, Method method) {
}
