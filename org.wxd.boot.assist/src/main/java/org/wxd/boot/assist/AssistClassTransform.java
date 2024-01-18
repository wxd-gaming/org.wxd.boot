package org.wxd.boot.assist;

import javassist.CtBehavior;
import javassist.CtClass;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * 包装类，可以通过字节码增加
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2024-01-05 20:49
 **/
public class AssistClassTransform implements ClassFileTransformer {

    /** 增强类所在包名白名单 */
    private final Set<String> Filter_PACKAGE = new HashSet<>();
    private final JavaAssistBox javaAssistBox = JavaAssistBox.of();

    public AssistClassTransform(String filterPackage) {
        if (filterPackage != null && !filterPackage.isEmpty() && !filterPackage.isBlank()) {
            String[] split = filterPackage.split("\\|");
            for (String string : split) {
                Filter_PACKAGE.add(string);
            }
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer) {
        try {
            if (className == null || className.isEmpty() || className.isBlank()) return classFileBuffer;
            final String finalClassName = className.replace("/", ".");
            if (
                    finalClassName.startsWith("jdk.")
                            || finalClassName.startsWith("java.")
                            || finalClassName.startsWith("javassist.")
                            || finalClassName.startsWith("javax.")
                            || finalClassName.startsWith("junit.")
                            || finalClassName.startsWith("sun.")
                            || finalClassName.startsWith("com.sun.")
                            || finalClassName.startsWith("ch.qos.")
                            || finalClassName.startsWith("org.slf4j.")
                            || finalClassName.startsWith("org.junit.")
                            || finalClassName.startsWith("com.intellij")
                            || finalClassName.startsWith("sun.launcher.")
                            || finalClassName.startsWith("org.hamcrest.")
                            || finalClassName.startsWith("net.sf.cglib.")
                            || finalClassName.startsWith("com.google.")
                            || finalClassName.startsWith("io.netty.")
                            || finalClassName.startsWith("com.alibaba.")
                            || finalClassName.startsWith("org.apache.")

                            || finalClassName.startsWith("org.jctools.")

                            || finalClassName.startsWith("redis.")

                            || finalClassName.startsWith("com.zaxxer.")
                            || finalClassName.startsWith("com.mysql.")

                            || finalClassName.startsWith("com.bson.")
                            || finalClassName.startsWith("com.mongodb.")

                            || finalClassName.startsWith("org.openjdk.")
                            || finalClassName.startsWith("org.objectweb.asm.")

                            || finalClassName.startsWith("org.wxd.boot.agent.")
                            || finalClassName.startsWith("org.wxd.boot.assist.")
                            || finalClassName.startsWith("org.wxd.boot.starter.")
                            || (!Filter_PACKAGE.isEmpty() && Filter_PACKAGE.stream().noneMatch(finalClassName::startsWith)))
                return classFileBuffer;

            AssistMonitor.printError("[" + AssistMonitor.SIMPLE_DATE_FORMAT.format(new Date()) + "] " + className);

            JavaAssistBox.JavaAssist javaAssist = javaAssistBox.editClass(finalClassName);

            boolean checked = check(javaAssist.getCtClass(), IAssistMonitor.class.getName());

            CtBehavior[] behaviors = javaAssist.getCtClass().getDeclaredBehaviors();
            //遍历方法进行增强
            for (CtBehavior m : behaviors) {
                MonitorAlligator monitorAlligator = (MonitorAlligator) m.getAnnotation(MonitorAlligator.class);
                if (monitorAlligator != null) continue;
                if (checked) {
                    enhanceStartMethod(finalClassName, m);
                } else {
                    enhanceStartMethod2(finalClassName, m);
                }
            }
            if (check(javaAssist.getCtClass(), IAssistOutFile.class.getName())) {
                /* 输出修改后的class文件内容 */
                //System.out.println(className + " - out");
                javaAssist.writeFile(AssistMonitor.ASSIST_OUT_DIR);
            }
            return javaAssist.toBytes();
        } catch (Throwable e) {
            AssistMonitor.printError("[" + AssistMonitor.SIMPLE_DATE_FORMAT.format(new Date()) + "] " + className, e);
        }
        return classFileBuffer;
    }

    /***
     *
     * @param sourceClass 原始类
     * @param implClass 需要查找的实现类
     * @return
     * @throws Exception
     */
    private boolean check(CtClass sourceClass, String implClass) throws Exception {
        for (CtClass anInterface : sourceClass.getInterfaces()) {
            if (implClass.equals(anInterface.getName())) {
                return true;
            }
            if (check(anInterface, implClass)) {
                return true;
            }
        }
        if (sourceClass.getSuperclass() != null) {
            return check(sourceClass.getSuperclass(), implClass);
        }
        return false;
    }

    /** 方法增强，添加方法耗时统计 */
    private void enhanceStartMethod(String className, CtBehavior method) {
        if (method.isEmpty()) {
            /*空方法，没意义*/
            return;
        }
        String methodName = method.getName();
        try {
            if (method.getMethodInfo().isStaticInitializer()) {
                /*如果这不是构造函数或类初始值设定项（静态初始值设定项）*/
                return;
            }
            JavaAssistBox.JavaAssist assist = javaAssistBox.editClass(MonitorRecord.MonitorStack.class.getName());
            method.addLocalVariable("monitorStack", assist.getCtClass());
            method.insertBefore(String.format("monitorStack = %s.start();", AssistMonitor.class.getName()));
            String str = """                    
                    %s.close(monitorStack, this);
                    """
                    .formatted(AssistMonitor.class.getName());
            method.insertAfter(str);
        } catch (Throwable e) {
            AssistMonitor.printError("[" + AssistMonitor.SIMPLE_DATE_FORMAT.format(new Date()) + "] " + className + "." + methodName, e);
        }
    }

    /** 方法增强，添加方法耗时统计 */
    private void enhanceStartMethod2(String className, CtBehavior method) {
        if (method.isEmpty()) {
            /*空方法，没意义*/
            return;
        }
        String methodName = method.getName();
        try {
            if (method.getMethodInfo().isStaticInitializer()) {
                /*如果这不是构造函数或类初始值设定项（静态初始值设定项）*/
                return;
            }
            JavaAssistBox.JavaAssist assist = javaAssistBox.editClass(MonitorRecord.MonitorStack.class.getName());
            method.addLocalVariable("monitorStack", assist.getCtClass());
            method.insertBefore(String.format("monitorStack = %s.start();", AssistMonitor.class.getName()));
            String str = """                    
                    %s.close(monitorStack);
                    """
                    .formatted(AssistMonitor.class.getName());
            method.insertAfter(str);
        } catch (Throwable e) {
            AssistMonitor.printError("[" + AssistMonitor.SIMPLE_DATE_FORMAT.format(new Date()) + "] " + className + "." + methodName, e);
        }
    }

}
