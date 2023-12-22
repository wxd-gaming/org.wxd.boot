package org.wxd.boot.net.controller;

import org.wxd.boot.net.controller.ann.TextMapping;

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
        String serviceName, String path, String remarks, Object instance,
        Method method) {
}
