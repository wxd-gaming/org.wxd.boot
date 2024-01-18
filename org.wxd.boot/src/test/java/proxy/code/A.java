package proxy.code;

import java.io.Serializable;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-08-19 09:58
 **/
public class A implements Serializable, IA {

    private static final long serialVersionUID = 1L;

    public A() {
        System.out.println("A");
    }

    public A(Object obj) {
        System.out.println("B");
    }

    public void test1(String v1) {
    }

    public String test2(String v1, String v2) throws Exception {
        return v1 + "_" + v2;
    }

    public <R> R test3(R v1) {
        return v1;
    }

}
