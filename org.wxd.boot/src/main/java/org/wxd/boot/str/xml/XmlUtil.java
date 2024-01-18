package org.wxd.boot.str.xml;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;
import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.agent.io.FileUtil;
import org.wxd.boot.agent.io.FileWriteUtil;
import org.wxd.boot.agent.lang.Record2;
import org.wxd.boot.agent.zip.ZipUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-07-29 10:33
 */
public class XmlUtil {

    private static final ThreadLocal<Serializer> SIMPLE_XM_LOCAL = new ThreadLocal<>();

    /**
     * 线程安全的序列化
     *
     * @return
     */
    public static Serializer serializerThreadLocal() {
        Serializer serializer = SIMPLE_XM_LOCAL.get();
        if (serializer == null) {
            String formatHead = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
            formatHead += "\n";
            formatHead += "<!-- @author: Troy.Chen(無心道, 15388152619) -->";
            Format format = new Format(formatHead);
            serializer = new Persister(format);
            SIMPLE_XM_LOCAL.set(serializer);
        }
        return serializer;
    }


    /**
     * 读取XML文件，加载进相应Object类型
     */
    public static <T> T fromXml4File(String path, Class<T> type) {
        final Record2<String, InputStream> inputStream = FileUtil.findInputStream(path);
        if (inputStream == null) throw new RuntimeException("找不到文件：" + path);
        return fromXml(inputStream.t1(), type);
    }

    /**
     * 读取XML文件，加载进相应Object类型
     */
    public static <T> T fromXml(InputStream stream, Class<T> type) {
        try {
            return serializerThreadLocal().read(type, stream, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * simpleXml
     *
     * @param <T>
     * @param source
     * @param type
     * @return
     */
    public static <T> T fromXml(String source, Class<T> type) {
        try {
            return serializerThreadLocal().read(type, source, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * simpleXml
     *
     * @param <T>
     * @param source
     * @param type
     * @return
     */
    public static <T> T fromXml(byte[] source, Class<T> type) {
        try (InputStream inputStream = new ByteArrayInputStream(source)) {
            return serializerThreadLocal().read(type, inputStream, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * simpleXml
     *
     * @param obj
     * @return
     */
    public static String toXml(Object obj) {
        try (StringWriter writer = new StringWriter()) {
            serializerThreadLocal().write(obj, writer);
            return writer.toString();
        } catch (Exception e) {
            throw Throw.as(e);
        }
    }

    /**
     * 将object类型转换为xml类型，并写入XML文件(其他格式也可以，比如txt文件)
     *
     * @param obj
     * @param path
     */
    public static void writerXmlFile(Object obj, String path) {
        String xml = toXml(obj);
        FileWriteUtil.writeString(path, xml);
    }

    /**
     * 把xml数据写到zip文件中
     *
     * @param object
     * @param pathString
     */
    public static void writerXMLToZipFile(Object object, String pathString, String fileName) {
        String toXMLString = toXml(object);
        ZipUtil.zip2File(pathString, fileName, toXMLString);
    }

}
