package org.wxd.boot.net.controller;

import org.wxd.boot.collection.concurrent.ConcurrentTable;
import org.wxd.boot.str.StringUtil;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 映射工程
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-12-12 20:06
 **/
public class MappingFactory {

    public static final String FINAL_DEFAULT = "default";
    /** 消息id -> 映射 */
    public static final ConcurrentTable<String, Integer, ProtoMappingRecord> PROTO_MAP = new ConcurrentTable<>();
    /** 路由 -> 映射 */
    public static final ConcurrentTable<String, String, TextMappingRecord> TEXT_MAP = new ConcurrentTable<>();

    public static void putProto(String serviceName, String remarks, int messageId, Object instance, Method method) {
        /** 虚拟线程 */
        if (StringUtil.emptyOrNull(serviceName)) serviceName = FINAL_DEFAULT;
        PROTO_MAP.put(
                serviceName,
                messageId,
                new ProtoMappingRecord(serviceName, remarks, messageId, instance, method)
        );
    }

    public static void putText(String serviceName, String remarks, String path, Object instance, Method method) {
        /** 虚拟线程 */
        if (StringUtil.emptyOrNull(serviceName)) serviceName = FINAL_DEFAULT;
        TEXT_MAP.put(
                serviceName,
                path,
                new TextMappingRecord(serviceName, remarks, path, instance, method)
        );
    }

    public static ProtoMappingRecord protoMappingRecord(String serviceName, int messageId) {
        ProtoMappingRecord mapping = PROTO_MAP.get(serviceName, messageId);
        if (mapping == null) {
            mapping = PROTO_MAP.get(FINAL_DEFAULT, messageId);
        }
        return mapping;
    }

    public static TextMappingRecord textMappingRecord(String serviceName, String path) {
        TextMappingRecord mapping = TEXT_MAP.get(serviceName, path);
        if (mapping == null) {
            mapping = TEXT_MAP.get(FINAL_DEFAULT, path);
        }
        return mapping;
    }

    public static Stream<ProtoMappingRecord> protoMappingRecord(String serviceName) {
        Stream<ProtoMappingRecord> stream = PROTO_MAP.opt(MappingFactory.FINAL_DEFAULT).map(Map::values).stream().flatMap(Collection::stream);
        if (!FINAL_DEFAULT.equalsIgnoreCase(serviceName)) {
            stream = Stream.concat(stream, PROTO_MAP.opt(serviceName).map(Map::values).stream().flatMap(Collection::stream));
        }
        return stream;
    }

    public static Stream<TextMappingRecord> textMappingRecord(String serviceName) {
        Stream<TextMappingRecord> stream = TEXT_MAP.opt(MappingFactory.FINAL_DEFAULT).map(Map::values).stream().flatMap(Collection::stream);
        if (!FINAL_DEFAULT.equalsIgnoreCase(serviceName)) {
            stream = Stream.concat(stream, TEXT_MAP.opt(serviceName).map(Map::values).stream().flatMap(Collection::stream));
        }
        return stream;
    }

}
