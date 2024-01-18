package org.wxd.boot.assist;

import javassist.*;
import lombok.Getter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * javassist 代码编辑器
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-12-06 20:07
 **/
@Getter
public class JavaAssistBox extends ClassLoader {

    public static JavaAssistBox of() {
        return new JavaAssistBox();
    }

    final AtomicInteger ATOMIC_INTEGER;
    final ClassPool CLASS_POOL;

    private JavaAssistBox() {
        ATOMIC_INTEGER = new AtomicInteger();
        CLASS_POOL = ClassPool.getDefault();
        try {
            CLASS_POOL.insertClassPath("./class");
        } catch (NotFoundException e) {
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            CtClass cc = CLASS_POOL.get(name);
            byte[] b = cc.toBytecode();
            return defineClass(name, b, 0, b.length);
        } catch (NotFoundException | IOException | CannotCompileException e) {
            throw new ClassNotFoundException();
        }
    }

    /**
     * 回调
     *
     * @author: Troy.Chen(無心道, 15388152619)
     * @version: 2024-01-05 21:16
     **/
    public interface Call<T> {
        void accept(T t) throws Exception;
    }

    @Getter
    public static class JavaAssist {

        private final JavaAssistBox javaAssistBox;
        private final CtClass ctClass;

        private JavaAssist(JavaAssistBox javaAssistBox, CtClass ctClass) {
            this.javaAssistBox = javaAssistBox;
            this.ctClass = ctClass;
        }

        /** 查询已有的方法 */
        public JavaAssist declaredMethod(String methodName, CtClass[] methodParams, Call<CtMethod> call) {
            try {
                CtMethod ctMethod = ctClass.getDeclaredMethod(methodName, methodParams);
                call.accept(ctMethod);
                return this;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * 创建一个方法
         *
         * @param modifier     Modifier.PUBLIC
         * @param returnType   方法的返回类型
         * @param methodName   方法的名字
         * @param methodParams 方法参数类型
         * @param call         回调
         */
        public JavaAssist createMethod(int modifier, CtClass returnType,
                                       String methodName, CtClass[] methodParams,
                                       Call<CtMethod> call) {
            try {
                CtMethod ctMethod = new CtMethod(returnType, methodName, methodParams, ctClass);
                ctMethod.setModifiers(modifier);
                call.accept(ctMethod);
                ctClass.addMethod(ctMethod);
                return this;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** 完整的方法字符串 */
        public JavaAssist createMethod(String methodStr) {
            try {
                CtMethod method = CtNewMethod.make(methodStr, ctClass);
                ctClass.addMethod(method);
                return this;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public JavaAssist writeFile(String directoryName) {
            try {
                getCtClass().writeFile(directoryName);
                return this;
            } catch (CannotCompileException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        public byte[] toBytes() {
            try {
                return getCtClass().toBytecode();
            } catch (CannotCompileException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        /** 通过 classloader 加载类 */
        public Class loadClass() {
            try {
                return javaAssistBox.loadClass(ctClass.getName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        /** 通过 classloader 加载类 */
        public JavaAssist call(Call<Class<?>> call) {
            try {
                Class<?> aClass = loadClass();
                call.accept(aClass);
                return this;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public <R> R toInstance() {
            try {
                return (R) loadClass().getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** 查找某个类编辑 */
    public JavaAssist editClass(Class<?> clazz) {
        return editClass(clazz.getName());
    }

    /** 查找某个类编辑 */
    public JavaAssist editClass(String clazzName) {
        try {
            CtClass tmp = CLASS_POOL.get(clazzName);
            return new JavaAssist(this, tmp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 继承某个类的实现 */
    public JavaAssist extendSuperclass(Class<?> superclass) {
        try {
            CtClass tmp = CLASS_POOL.makeClass(superclass.getName() + "Impl" + ATOMIC_INTEGER.incrementAndGet());
            tmp.setSuperclass(CLASS_POOL.get(superclass.getName()));
            return new JavaAssist(this, tmp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 实现某个接口的 */
    public JavaAssist implInterfaces(String className, Class<?>[] interfaces) {
        try {
            CtClass tmp = CLASS_POOL.makeClass(className + "Impl" + ATOMIC_INTEGER.incrementAndGet());
            for (Class<?> aClass : interfaces) {
                tmp.addInterface(CLASS_POOL.get(aClass.getName()));
            }
            return new JavaAssist(this, tmp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 创建一个类 */
    public JavaAssist create(String className) {
        try {
            CtClass tmp = CLASS_POOL.makeClass(className + "Impl" + ATOMIC_INTEGER.incrementAndGet());
            return new JavaAssist(this, tmp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
