package com.test.lambda;

import lombok.Getter;
import lombok.Setter;
import org.junit.Test;
import org.wxd.boot.agent.function.*;
import org.wxd.boot.agent.system.LambdaUtil;

import java.io.Serializable;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-07-27 10:36
 **/
@Getter
@Setter
public class LambdaTest implements Serializable {

    private boolean isOpen;
    private String strName;
    private String getStrName;

    public static void t1() {}

    public void t2(String str) {
        System.out.println(str);
    }


    @Test
    public void g() throws Exception {
        LambdaTest lambdaTest = new LambdaTest();
        ConsumerE0 t1 = LambdaTest::t1;
        ConsumerE1<String> t2 = lambdaTest::t2;
        t2.accept("2");
        t1.accept();
        ConsumerE0 g = lambdaTest::g;

        STVFunction1<LambdaTest, String> setGetStrName = lambdaTest::setGetStrName;
        setGetStrName.apply("1");
        System.out.println(setGetStrName.ofMethodName());
        System.out.println(lambdaTest.getGetStrName());

        ConsumerE1<String> setGetStrName1 = lambdaTest::setGetStrName;

        SLFunction0<String> getGetStrName = lambdaTest::getGetStrName;

        SLFunction1<LambdaTest, Boolean> isOpen1 = LambdaTest::isOpen;

        System.out.println(LambdaUtil.ofField(isOpen1));
        System.out.println(LambdaUtil.ofField(LambdaTest::getStrName));
        System.out.println(LambdaUtil.ofField(LambdaTest::getGetStrName));
    }

}
