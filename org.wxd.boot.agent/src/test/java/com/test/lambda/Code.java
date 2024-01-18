package com.test.lambda;

import lombok.Getter;
import lombok.Setter;
import org.wxd.boot.agent.function.ConsumerE1;
import org.wxd.boot.agent.function.SerializableLambda;

import java.lang.invoke.SerializedLambda;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-10-20 19:28
 **/
@Getter
@Setter
public class Code {

    interface Ti {
        void ti(String s);
    }

    @Getter
    @Setter
    class A implements Ti {
        private int i2;

        @Override
        public void ti(String s) {
            System.out.println("输出：" + this.getClass().getName() + " - " + s);
        }
    }

    @Getter
    @Setter
    class B implements Ti {
        private int i3;

        @Override
        public void ti(String s) {
            System.out.println("输出：" + this.getClass().getName() + " - " + s);
        }
    }

    public static void main(String[] args) throws Exception {
        Code code = new Code();
        B b = code.new B();
        ConsumerE1<String> stringConsumer = b::ti;
        SerializedLambda serializedLambda = SerializableLambda.getSerializedLambda(stringConsumer);
        System.out.println(serializedLambda.getImplMethodName());
        System.out.println(SerializableLambda.ofMethod(serializedLambda));

//        System.out.println(LambdaUtil.ofMethod(code::div));
//        System.out.println(LambdaUtil.ofMethod(code::test));
//        System.out.println(LambdaUtil.ofMethod(code::getI1));
//        System.out.println(LambdaUtil.ofMethod(code::setI1));
//        System.out.println(LambdaUtil.ofMethod(code::test2));
//        System.out.println(LambdaUtil.ofMethod(code::test3));
//        Method x1 = LambdaUtil.ofMethod(Ti::ti);
//        System.out.println(x1);
//        x1.invoke(b, "1");
//        System.out.println(AnnUtil.annotations(x1, Sort.class));
//        Method x = LambdaUtil.ofMethod(B::ti);
//        System.out.println(x);
//        System.out.println(AnnUtil.annotations(x, Sort.class));
    }

    private String s1;
    private int i1;

    public void test3() {}

    public void test2(int i1) {}

    public double div(int a, int b) {
        return a * 1.0 / b;
    }

    public double test(int o1, int o2, int o3) {
        return 0d;
    }
}
