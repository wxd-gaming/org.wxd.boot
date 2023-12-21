package org.wxd.boot.agent.system;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.slf4j.LoggerFactory;
import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.agent.loader.ClassDirLoader;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * 资源处理
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-10-16 10:11
 **/
@Getter
public class ReflectContext {

    /** 判定 接口, 枚举, 注解, 抽象类 返回 false */
    public static boolean checked(Class<?> aClass) {
        return checked(null, aClass);
    }

    /** 判定 接口, 枚举, 注解, 抽象类 返回 false */
    public static boolean checked(Class<?> cls, Class<?> aClass) {
        /* 判定 是否可用 */
        return !(
                Object.class.equals(aClass)
                        || aClass.isInterface()
                        || aClass.isEnum()
                        || aClass.isAnnotation()
                        || Modifier.isAbstract(aClass.getModifiers())
        );
    }

    /** 获取类型实现的接口 */
    public static Collection<Class<?>> getInterfaces(Class<?> cls) {
        TreeMap<String, Class<?>> classCollection = new TreeMap<>();
        getInterfaces(classCollection, cls);
        return new ArrayList<>(classCollection.values());
    }

    /** 获取类实现的接口 */
    public static void getInterfaces(TreeMap<String, Class<?>> classCollection, Class<?> cls) {
        if (cls == null || Object.class.equals(cls)) {
            return;
        }
        /*查找父类*/
        getInterfaces(classCollection, cls.getSuperclass());
        Class<?>[] interfaces = cls.getInterfaces();
        for (Class<?> aInterface : interfaces) {
            /*查找接口，实现的接口*/
            getInterfaces(classCollection, aInterface);
            /*查找父类*/
            getInterfaces(classCollection, aInterface.getSuperclass());
            if (aInterface.isInterface()) {
                classCollection.put(aInterface.getName(), aInterface);
            }
        }
    }

    /** 获取泛型的第一个 */
    public static Class<?> getTClass(Class<?> source) {
        return getTClass(source, 0);
    }

    /** 获取泛型的类型 */
    public static Class<?> getTClass(Class<?> source, int index) {
        Type genType = source.getGenericSuperclass();
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
        return (Class<?>) params[index];
    }

    /** 所有的类 */
    private final List<Content> contentList;

    public ReflectContext(Collection<Class<?>> contentList) {
        this.contentList = contentList.stream().map(Content::new).toList();
    }

    public Stream<Content> stream() {
        return contentList.stream();
    }

    /** 父类或者接口 */
    public Stream<Content> withSuper(Class<?> cls) {
        return withSuper(cls, null);
    }

    /** 父类或者接口 */
    public Stream<Content> withSuper(Class<?> cls, Predicate<Class<?>> predicate) {
        Stream<Content> tmp = stream().filter(v -> cls.isAssignableFrom(v.getCls()));
        if (predicate != null) tmp = tmp.filter(v -> predicate.test(v.getCls()));
        return tmp;
    }

    /** 所有添加了这个注解的类 */
    public Stream<Content> withAnnotated(Class<? extends Annotation> annotation) {
        return withAnnotated(annotation, null);
    }

    public Stream<Content> withAnnotated(Class<? extends Annotation> annotation, Predicate<Class<?>> predicate) {
        Stream<Content> tmp = stream().filter(c -> AnnUtil.ann(c.getCls(), annotation) != null);
        if (predicate != null) tmp = tmp.filter(v -> predicate.test(v.getCls()));
        return tmp;
    }

    public Stream<Class<?>> classStream() {
        return contentList.stream().map(Content::getCls);
    }

    /** 父类或者接口 */
    public Stream<Class<?>> classWithSuper(Class<?> cls) {
        return classWithSuper(cls, null);
    }

    /** 父类或者接口 */
    public Stream<Class<?>> classWithSuper(Class<?> cls, Predicate<Class<?>> predicate) {
        Stream<Class<?>> tmp = classStream().filter(cls::isAssignableFrom);
        if (predicate != null) tmp = tmp.filter(predicate);
        return tmp;
    }

    /** 所有添加了这个注解的类 */
    public Stream<Class<?>> classWithAnnotated(Class<? extends Annotation> annotation) {
        return classWithAnnotated(annotation, null);
    }

    /** 所有添加了这个注解的类 */
    public Stream<Class<?>> classWithAnnotated(Class<? extends Annotation> annotation, Predicate<Class<?>> predicate) {
        Stream<Class<?>> tmp = classStream().filter(c -> AnnUtil.ann(c, annotation) != null);
        if (predicate != null) tmp = tmp.filter(predicate);
        return tmp;
    }

    @Getter
    public static class Content {

        private final Class<?> cls;

        public static Content of(Class<?> cls) {
            return new Content(cls);
        }

        Content(Class<?> cls) {
            this.cls = cls;
        }

        public Collection<Method> getMethods() {
            return MethodUtil.readAllMethod(cls).values();
        }

        public Stream<Method> methodStream() {
            return getMethods().stream();
        }

        /** 所有添加了这个注解的方法 */
        public Stream<Method> methodsWithAnnotated(Class<? extends Annotation> annotation) {
            return methodStream().filter(m -> AnnUtil.ann(m, annotation) != null);
        }

        public Collection<Field> getFields() {
            return FieldUtil.getFields(false, cls).values();
        }

        /** 所有添加了这个注解的方法 */
        public Stream<Field> fieldStream() {
            return getFields().stream();
        }

        /** 所有添加了这个注解的字段 */
        public Stream<Field> fieldWithAnnotated(Class<? extends Annotation> annotation) {
            return fieldStream().filter(f -> AnnUtil.ann(f, annotation) != null);
        }

    }

    @Setter
    @Accessors(chain = true)
    public static class Builder {

        public static Builder of(String... packageNames) {
            return of(Thread.currentThread().getContextClassLoader(), packageNames);
        }

        public static Builder of(ClassLoader classLoader, String... packageNames) {
            return new Builder(classLoader, packageNames);
        }

        private final ClassLoader classLoader;
        private final String[] packageNames;
        /** 是否读取子包 */
        private boolean findChild = true;
        /** 查找类的时候忽略接口 */
        private boolean filterInterface = true;
        /** 过滤掉抽象类 */
        private boolean filterAbstract = true;
        /** 过滤掉枚举类 */
        private boolean filterEnum = true;

        private Builder(ClassLoader classLoader, String[] packageNames) {
            this.classLoader = classLoader;
            this.packageNames = packageNames;
        }

        /** 所有的类 */
        public ReflectContext build() {
            TreeMap<String, Class<?>> classCollection = new TreeMap<>();
            for (String packageName : packageNames) {
                findClasses(packageName, aClass -> classCollection.put(aClass.getName(), aClass));
            }
            List<Class<?>> list = classCollection.values()
                    .stream()
                    .filter(v -> !Object.class.equals(v))
                    .filter(v -> !filterInterface || !v.isInterface())
                    .filter(v -> !filterAbstract || !Modifier.isAbstract(v.getModifiers()))
                    .filter(v -> !filterEnum || !v.isEnum())
                    .filter(v -> !v.isAnnotation())
                    .toList();
            return new ReflectContext(list);
        }

        private void findClasses(String packageName, Consumer<Class<?>> consumer) {
            String packagePath = packageName;
            if (packageName.endsWith(".jar") || packageName.endsWith(".war")) {
                packagePath = packageName;
            } else if (!".".equals(packageName)) {
                packagePath = packageName.replace(".", "/");
            }
            try {
                if (classLoader instanceof ClassDirLoader dirLoader) {
                    final Collection<Class<?>> classes = dirLoader.getLoadClassMap().values();
                    if (!classes.isEmpty()) {
                        for (Class<?> aClass : classes) {
                            if (aClass.getName().startsWith(packageName)) {
                                consumer.accept(aClass);
                            }
                        }
                    }
                }
                Enumeration<URL> resources = classLoader.getResources(packagePath);
                if (resources != null) {
                    URL url = null;
                    while (resources.hasMoreElements()) {
                        url = resources.nextElement();
                        if (url != null) {
                            String type = url.getProtocol();
                            String urlPath = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8);
                            if (type.equals("file")) {
                                String dir = urlPath.substring(0, urlPath.lastIndexOf(packagePath));
                                findClassByFile(dir, urlPath, consumer);
                            } else if (type.equals("jar") || type.equals("zip")) {
                                findClassByJar(urlPath, consumer);
                            }
                        } else {
                            findClassByJars(
                                    ((URLClassLoader) classLoader).getURLs(),
                                    packagePath,
                                    consumer
                            );
                        }
                    }
                }
            } catch (Throwable e) {
                throw Throw.as(e);
            }
        }

        /**
         * 从项目文件获取某包下所有类
         *
         * @param dir      父级文件夹
         * @param filePath 文件路径
         */
        private void findClassByFile(String dir, String filePath, Consumer<Class<?>> consumer) {
            File file = new File(filePath);
            File[] childFiles = file.listFiles();
            if (childFiles != null) {
                for (File childFile : childFiles) {
                    if (childFile.isDirectory()) {
                        if (findChild) {
                            findClassByFile(dir, childFile.getPath(), consumer);
                        }
                    } else {
                        String childFilePath = childFile.getPath();
                        if (childFilePath.endsWith(".class")) {
                            childFilePath = childFilePath.substring(dir.length() - 1, childFilePath.lastIndexOf("."));
                            childFilePath = childFilePath.replace("\\", ".");

                            loadClass(childFilePath, consumer);

                        }
                    }
                }
            }
        }

        /**
         * 从所有jar中搜索该包，并获取该包下所有类
         *
         * @param urls        URL集合
         * @param packagePath 包路径
         */
        private void findClassByJars(URL[] urls, String packagePath, Consumer<Class<?>> consumer) {

            if (urls != null) {
                for (URL url : urls) {
                    String urlPath = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8);
                    // 不必搜索classes文件夹
                    if (urlPath.endsWith("classes/")) {
                        continue;
                    }
                    String jarPath = urlPath + "!/" + packagePath;
                    findClassByJar(jarPath, consumer);
                }
            }
        }

        /**
         * 从jar获取某包下所有类
         *
         * @param jarPath jar文件路径
         */
        private void findClassByJar(String jarPath, Consumer<Class<?>> consumer) {

            String[] jarInfo = jarPath.split("!");
            String jarFilePath = jarInfo[0].substring(jarInfo[0].indexOf("/"));
            String packagePath = jarInfo[1].substring(1);
            String entryName;
            try (JarFile jarFile = new JarFile(jarFilePath)) {
                Enumeration<JarEntry> entrys = jarFile.entries();
                while (entrys.hasMoreElements()) {
                    JarEntry jarEntry = entrys.nextElement();
                    entryName = jarEntry.getName();
                    if (entryName.endsWith(".class")) {
                        if (findChild) {
                            if (!entryName.startsWith(packagePath)) {
                                continue;
                            }
                        } else {
                            int index = entryName.lastIndexOf("/");
                            String myPackagePath;
                            if (index != -1) {
                                myPackagePath = entryName.substring(0, index);
                            } else {
                                myPackagePath = entryName;
                            }
                            if (!myPackagePath.equals(packagePath)) {
                                continue;
                            }
                        }
                        entryName = entryName.replace("/", ".").substring(0, entryName.lastIndexOf("."));
                        loadClass(entryName, consumer);
                    }
                }
            } catch (Throwable e) {
                throw Throw.as(jarPath, e);
            }
        }

        private void loadClass(String childFilePath, Consumer<Class<?>> consumer) {
            try {
                Class<?> clazz = classLoader.loadClass(childFilePath);
                consumer.accept(clazz);
            } catch (Throwable e) {
                LoggerFactory.getLogger(ReflectContext.class).error(childFilePath, e);
            }
        }

    }

}
