package code;

import java.lang.annotation.*;

public class Code2 {

    //// 定义一个基本注解
    //@Retention(RetentionPolicy.RUNTIME)
    //@Target({ElementType.TYPE}) // 只能应用于类、接口等类型
    //public @interface BaseAnnotation {
    //    String value();
    //}
    //
    //// 定义一个继承BaseAnnotation的子注解
    //@Retention(RetentionPolicy.RUNTIME)
    //@Target({ElementType.TYPE})
    //@Inherited // 表明这个注解可以被继承
    //public @interface ChildAnnotation extends BaseAnnotation {
    //    int count() default 0;
    //}
    //
    //// 测试类
    //@ChildAnnotation("Hello")
    //static class MyClass {}
    //
    //public class Main {
    //    public static void main(String[] args) throws NoSuchFieldException {
    //        Class<MyClass> clazz = MyClass.class;
    //
    //        if (clazz.isAnnotationPresent(ChildAnnotation.class)) {
    //            ChildAnnotation annotation = clazz.getAnnotation(ChildAnnotation.class);
    //
    //            System.out.println("Value: " + annotation.value());
    //            System.out.println("Count: " + annotation.count());
    //        } else {
    //            System.out.println("No ChildAnnotation found.");
    //        }
    //    }
    //}
}
