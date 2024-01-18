package proxy;

import org.junit.Test;
import org.wxd.boot.str.PatternStringUtil;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-08-19 11:17
 **/
public class ProxyTest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Test
    public void print() throws NoSuchMethodException {
        println(byte.class);
        System.out.println(byte[].class.getName());
        System.out.println(Byte[].class.getName());
        println(byte[][].class);
        println(long.class);
        println(long[].class);
        println(long[][].class);
        println(Float.class);
        println(float.class);
        println(float[].class);
        println(float[][].class);
        println(Float[].class);
        println(ProxyTest[].class);
        println(String[].class);
        List<Boolean> bb = new ArrayList<>();
        System.out.println(PatternStringUtil.typeString(bb.getClass().getName()));
        System.out.println(PatternStringUtil.typeString(byte[].class.getSimpleName()));
        System.out.println(PatternStringUtil.typeString(byte[].class.getName()));
        System.out.println(PatternStringUtil.typeString(char[].class.getName()));
        System.out.println(PatternStringUtil.typeString(Charset[].class.getName()));
        System.out.println(PatternStringUtil.typeString(short[].class.getName()));
        System.out.println(PatternStringUtil.typeString(double[].class.getName()));
        System.out.println(PatternStringUtil.typeString(Double[].class.getName()));
        System.out.println(PatternStringUtil.typeString(boolean[].class.getName()));
        System.out.println(PatternStringUtil.typeString(Boolean[].class.getName()));
        System.out.println(PatternStringUtil.typeString(boolean[][].class.getName()));
        System.out.println(PatternStringUtil.typeString(Boolean[][].class.getName()));
        System.out.println(PatternStringUtil.typeString(ProxyTest[].class.getName()));
        Object tmp = new byte[2];
        System.out.println(tmp instanceof byte[]);
    }

    public static void println(Class<?> c) {
        Package aPackage = c.getPackage();
        String simpleName;
        if (aPackage != null) {
            simpleName = aPackage.getName() + "." + c.getSimpleName();
        } else {
            simpleName = c.getSimpleName();
        }
        System.out.println(simpleName);
    }

    public void test(Object obj, long[] ints, byte[] ints1, Boolean[] ints2, String... args) {
    }

}

