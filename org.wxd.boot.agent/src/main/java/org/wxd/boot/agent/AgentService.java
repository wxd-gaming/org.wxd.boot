package org.wxd.boot.agent;

import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.agent.io.FileReadUtil;
import org.wxd.boot.agent.io.FileUtil;
import org.wxd.boot.agent.io.FileWriteUtil;
import org.wxd.boot.agent.loader.ClassDirLoader;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 动态替换class类，只能修改方法体
 *
 * @author: 特别鸣谢 上海-念念（qq:596889735）
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-12-29 20:24
 **/
@Slf4j
public class AgentService implements Serializable {

    private static final String Full_Class_Name = "com.sun.tools.attach.VirtualMachine";

    public static String jdkHome() {
        return System.getProperty("java.ext.dirs");
    }

    public static String jreHome() {
        return System.getProperty("java.home");
    }

    public static String agentJarPath() {
        String agentJarPath = FileUtil.clazzJarPath(AgentService.class);
        if (agentJarPath == null || agentJarPath.isEmpty() || agentJarPath.isBlank()) {
            throw new NullPointerException("热更没有找到 agent jar 文件");
        }
        return agentJarPath;
    }

    /**
     * 热更类
     *
     * @param agentPath 执行热加载的class文件目录
     */
    public static String agentClass(String agentPath) throws Throwable {
        return agentClass(agentJarPath(), agentPath);
    }

    /**
     * 热更类
     *
     * @param agentJarPath agent Jar 路径
     * @param agentPath    执行热加载的class文件目录
     */
    public static String agentClass(String agentJarPath, String agentPath) throws Throwable {
        String pid = getPid();
        return agentClass(pid, agentJarPath, agentPath);
    }

    public static String agentClass(String pid, String agentJarPath, String agentPath) throws Throwable {
        Class<?> VirtualMachineClass = check(agentJarPath);
        log.error("进程id：" + pid);
        Method attach = VirtualMachineClass.getMethod("attach", String.class);
        com.sun.tools.attach.VirtualMachine vm = (com.sun.tools.attach.VirtualMachine) attach.invoke(null, pid);
        vm.loadAgent(agentJarPath, agentPath);
        vm.detach();
        return FileReadUtil.readString(agentPath + "/hot.log");
    }

    public static Class<?> check(String agentJarPath) throws IOException {
        final File file = new File(agentJarPath);
        if (!file.exists()) {
            throw new RuntimeException("agent jar 无法找到：" + agentJarPath);
        }

        String jreHome = jreHome();
        log.info("java_home：" + jreHome);
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        try {
            Class<?> aClass = systemClassLoader.loadClass(Full_Class_Name);
            log.warn("原始加载器加载成功：" + aClass);
            return aClass;
        } catch (Exception e) {
            /*1.8和之前的版本需要附加 tools.jar 到系统加载器，java11开始原始加载器，在 src.zip 文件里面*/
            if (systemClassLoader instanceof URLClassLoader) {

                /* 注意，是jre的bin目录，不是jdk的bin目录，VirtualMachine need the attach.dll in the jre of the JDK. */
                File toolsJarPath = FileUtil.walkFiles(jreHome + File.pathSeparator + "..", "lib/tools.jar")
                        .findFirst()
                        .orElse(null);

                log.info("java_home：" + toolsJarPath);

                try {
                    Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                    method.setAccessible(true);
                    method.invoke(systemClassLoader, file.toURI().toURL());
                    final Class<?> loadClass = systemClassLoader.loadClass(Full_Class_Name);
                    log.warn("附加外部 jar 包，加载器加载成功：" + loadClass);
                    return loadClass;
                } catch (Throwable throwable) {
                    throw new RuntimeException("ClassLoader 附加 jar 包", e);
                }

            }
            throw Throw.as(agentJarPath + " - " + Full_Class_Name, e);
        }
    }

    /**
     * agentArgs就是VirtualMachine.loadAgent()的第二个参数
     *
     * @param sourceDir 代理调用的热更java文件目录
     * @param inst      热更代理
     */
    public static void agentmain(String sourceDir, Instrumentation inst) throws Exception {

        StringBuilder stringBuilder = new StringBuilder();

        ClassDirLoader classFileLoader = new ClassDirLoader(sourceDir);
        Map<String, byte[]> classFileMap = classFileLoader.getClassFileMap();
        stringBuilder.append("路径 “").append(sourceDir).append("” 下 class 文件 数量：").append(classFileMap.size()).append("\n");
        for (Map.Entry<String, byte[]> entry : classFileMap.entrySet()) {
            /*这一步很关键，必须是从原始的classloader 获取原始的类*/
            List<Class> collect = Arrays.stream(inst.getAllLoadedClasses())
                    .filter(v -> v.getName().equalsIgnoreCase(entry.getKey()) || v.getSimpleName().equalsIgnoreCase(entry.getKey()))
                    .collect(Collectors.toList());
            if (collect == null || collect.isEmpty()) {
                stringBuilder.append("需要被替换的原始类：").append(entry.getKey()).append(", 并未找到").append("\n");
                continue;
            }
            for (Class oldLoadClass : collect) {
                try {
                    /*把类的定义与新的类文件关联起来*/
                    ClassDefinition reporterDef = new ClassDefinition(oldLoadClass, entry.getValue());
                    inst.redefineClasses(reporterDef);
                    stringBuilder.append("成功热更新：").append(oldLoadClass.getName()).append(" - ").append(oldLoadClass.hashCode()).append("\n");
                } catch (Throwable e) {
                    stringBuilder.append(entry.getKey()).append("\n").append(Throw.ofString(e)).append("\n");
                }
            }
        }
        String toString = stringBuilder.toString();
        log.warn(toString);
        FileWriteUtil.writeString(sourceDir + "/hot.log", toString);
    }

    /**
     * 程序的进程id
     *
     * @return
     */
    public static String getPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return name.split("@")[0];
    }

}
