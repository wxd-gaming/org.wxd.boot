package com.test;

import org.junit.Test;
import org.wxd.boot.agent.exception.Throw;

/**
 * 测试代码
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-10-26 10:46
 **/
public class ThrowTest {

    @Test
    public void c1() {
        new RuntimeException(new RuntimeException("e")).printStackTrace();
        try {
            t1();
        } catch (Exception e) {
            Throw.as(e).printStackTrace();
        }
    }

    public void t1() throws Exception {
        try {
            t2();
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public void t2() throws Exception {
        throw new Exception();
    }
}
