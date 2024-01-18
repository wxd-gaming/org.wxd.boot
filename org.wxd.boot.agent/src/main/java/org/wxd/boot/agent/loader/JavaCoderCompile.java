package org.wxd.boot.agent.loader;


import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.JDKVersion;
import org.wxd.boot.agent.io.FileUtil;
import org.wxd.boot.agent.io.FileWriteUtil;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * java 文件编译
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-12-30 20:33
 **/
@Slf4j
public class JavaCoderCompile {

    /*获取编译器实例*/
    private final JavaCompiler compiler;
    private final DiagnosticCollector<JavaFileObject> oDiagnosticCollector;
    private JavaFileObjectManager javaFileManager = null;
    /**
     * spring 项目需要把主jar包解压，然后classpath指定 BOOT-INF/classes/ 目录
     * 比如 ./lib:./BOOT-INF/classes/
     */
    private String classPath = null;
    private ClassLoader parentClassLoader = null;

    public JavaCoderCompile() {
        compiler = ToolProvider.getSystemJavaCompiler();
        oDiagnosticCollector = new DiagnosticCollector<>();
    }

    public JavaCoderCompile classPath(String classPath) {
        this.classPath = classPath;
        return this;
    }

    public JavaCoderCompile parentClassLoader(ClassLoader parentClassLoader) {
        this.parentClassLoader = parentClassLoader;
        return this;
    }

    public JavaFileObjectManager javaFileManager() {
        if (javaFileManager == null) {
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(oDiagnosticCollector, null, StandardCharsets.UTF_8);
            javaFileManager = new JavaFileObjectManager(fileManager, parentClassLoader);
        }
        return javaFileManager;
    }

    /**
     * 加载java 源代码，并返回一个类
     *
     * @param javaCoder
     * @return
     */
    public JavaCoderCompile compilerCode(String javaCoder) throws Exception {
        //类全名
        String fullClassName = JavaFileObject4StringCode.readFullClassName(javaCoder);
        //构造源代码对象
        JavaFileObject javaFileObject = new JavaFileObject4StringCode(fullClassName, javaCoder);
        List<JavaFileObject> javaFileObjects = List.of(javaFileObject);
        compilerJava(null, javaFileObjects);
        return this;
    }

    /**
     * @param sourceDir 需要编译的文件路径
     */
    public JavaCoderCompile compilerJava(String sourceDir) throws Exception {
        final Collection<File> sourceFileList = FileUtil.walkFiles(sourceDir, ".java").collect(Collectors.toList());
        compilerJava(sourceDir, sourceFileList);
        return this;
    }

    /**
     * 需要编译的文件
     *
     * @param sourceDir      文件原路径
     * @param sourceFileList 文件列表
     * @return
     */
    public JavaCoderCompile compilerJava(String sourceDir, Collection<File> sourceFileList) throws Exception {
        if (!sourceFileList.isEmpty()) {
            final Iterable<? extends JavaFileObject> compilerFiles = javaFileManager().getSuperFileManager().getJavaFileObjectsFromFiles(sourceFileList);
            this.compilerJava(sourceDir, compilerFiles);
        }
        return this;
    }

    /**
     * @param sourceDir     可以null
     * @param compilerFiles 需要编译的文件
     */
    public JavaCoderCompile compilerJava(String sourceDir, Iterable<? extends JavaFileObject> compilerFiles) throws Exception {
        if (log.isDebugEnabled()) {
            JDKVersion jdkVersion = JDKVersion.runTimeJDKVersion();
            log.debug("compiler java file jdk_version：" + jdkVersion.getCurVersionString());
        }
        /**
         * 编译选项，在编译java文件时，
         * <p>
         *     编译程序会自动的去寻找java文件引用的其他的java源文件或者class。
         * <p>
         *     -sourcepath选项就是定义java源文件的查找目录，
         * <p>
         *     -classpath选项就是定义class文件的查找目录。
         */
        List<String> options = new LinkedList<>();
        options.add("-g");
        options.add("-Xdiags:verbose");/*完整信息*/
        options.add("-source");
        options.add(JDKVersion.runTimeJDKVersion().getVersionString());
        options.add("-encoding");
        options.add(StandardCharsets.UTF_8.toString());

        if (sourceDir != null) {
            options.add("-sourcepath");
            options.add(sourceDir); //指定文件目录
        }

        if (classPath != null) {
            options.add("-classpath");
            options.add(classPath); //指定文件目录
        }

        /*获取编译器实例*/
        JavaCompiler.CompilationTask compilationTask = compiler.getTask(
                null,
                javaFileManager(),
                oDiagnosticCollector,
                options,
                null,
                compilerFiles);
        // 运行编译任务
        Boolean call = compilationTask.call();
        if (!call) {
            StringBuilder sb = new StringBuilder();
            sb.append("编译异常：").append("\n");

            for (Diagnostic<? extends JavaFileObject> oDiagnostic : oDiagnosticCollector.getDiagnostics()) {
                sb
                        .append("\n").append(oDiagnostic.getKind().toString())
                        .append(" ：").append(oDiagnostic.getMessage(Locale.SIMPLIFIED_CHINESE));
                if (oDiagnostic.getSource() != null) {
                    sb
                            .append(" 文件：").append(oDiagnostic.getSource().getName());
                } else {
                    sb
                            .append(" 文件：未知");
                }
                sb
                        .append(" line:").append(oDiagnostic.getLineNumber())
                        .append(" pos:").append(oDiagnostic.getStartPosition());
            }

            throw new Exception(sb.toString());
        }
        return this;
    }

    /**
     * 导出文件
     *
     * @param outPath 路径
     * @return
     * @throws IOException
     */
    public JavaCoderCompile outPutFile(String outPath) throws Exception {
        final Map<String, byte[]> stringMap = toBytesMap();
        FileWriteUtil.writeClassFile(outPath, stringMap);
        return this;
    }

    /**
     * 当前编译器，所有类的加载器
     */
    public ClassBytesLoader builderClassLoader() {
        return new ClassBytesLoader(toBytesMap());
    }

    public ClassBytesLoader builderClassLoader(ClassLoader parent) {
        return new ClassBytesLoader(toBytesMap(), parent);
    }

    /**
     * 编译后所有的类
     *
     * @return
     */
    public Map<String, byte[]> toBytesMap() {
        return javaFileManager().getClassFileObjectLoader().toBytesMap();
    }

    /**
     * 获取所有的编译后的class
     */
    public Collection<ClassInfo> toAllClass() {
        return builderClassLoader().getLoadClassInfoMap().values();
    }

}
