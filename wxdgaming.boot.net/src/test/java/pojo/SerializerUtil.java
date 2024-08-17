package pojo;


import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import lombok.extern.slf4j.Slf4j;

/**
 * pojo 处理
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2024-08-17 21:03
 */
@Slf4j
public class SerializerUtil {


    public static <T> byte[] encode(T object) {
        Class<T> aClass = (Class<T>) object.getClass();
        return encode(object, aClass);
    }

    public static <T> byte[] encode(T object, Class<T> clazz) {
        Schema<T> schema = RuntimeSchema.getSchema(clazz);
        LinkedBuffer buffer = LinkedBuffer.allocate();
        return ProtostuffIOUtil.toByteArray(object, schema, buffer);
    }


    public static <T> T decode(byte[] bytes, Class<T> clazz) {
        T object;
        try {
            object = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception var4) {
            throw new RuntimeException("Protostuff反序列化时创建实例失败,Class:" + clazz.getName(), var4);
        }

        return decode(bytes, clazz, object);
    }

    public static <T> T decode(byte[] bytes, T object) {
        return decode(bytes, (Class<T>) object.getClass(), object);
    }

    public static <T> T decode(byte[] bytes, Class<T> clazz, T object) {
        Schema<T> schema = RuntimeSchema.getSchema(clazz);
        ProtostuffIOUtil.mergeFrom(bytes, object, schema);
        return object;
    }
}
