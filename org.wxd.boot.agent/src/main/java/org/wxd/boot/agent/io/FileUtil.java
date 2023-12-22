package org.wxd.boot.agent.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.agent.function.ConsumerE1;
import org.wxd.boot.agent.function.ConsumerE2;
import org.wxd.boot.agent.function.FunctionE;
import org.wxd.boot.agent.zip.ReadZipFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 文件辅助
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-08-18 14:40
 **/
public class FileUtil implements Serializable {

    private static Logger log() {
        return LoggerFactory.getLogger(FileUtil.class);
    }

    private static final long serialVersionUID = 1L;
    private static final String[] empty = new String[0];

    /**
     * 返回绝对路径
     */
    public static String getCanonicalPath(String fileName) {
        return getCanonicalPath(file(fileName));
    }

    /**
     * 返回绝对路径
     */
    public static String getCanonicalPath(File fileName) {
        try {
            return fileName.getCanonicalPath();
        } catch (Exception e) {
            throw Throw.as(e);
        }
    }

    /** 根据传入的类，获取类的jar包路径 */
    public static String clazzJarPath(Class clazz) {
        return clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
    }

    /**
     * 优先读取 config 目录下面
     * <p> 如果没有，从resources 文件夹下面读取
     */
    public static File findFile(String fileName) {
        return findFile(fileName, Thread.currentThread().getContextClassLoader());
    }

    public static File findFile(String fileName, ClassLoader classLoader) {

        fileName = fileName.replace("\\", "/");

        File file = new File(fileName);
        if (exists(file)) {
            return file;
        }

        if (!fileName.startsWith("config")) {
            file = new File("config/" + fileName);
            if (exists(file)) {
                return file;
            }
        }

        {
            /*todo 获取resource 不能是/开始的*/
            if (fileName.startsWith("/")) {
                fileName = fileName.substring(1);
            }
            URL resource = classLoader.getResource(fileName);
            if (resource != null) {
                return new File(resource.getFile());
            }

            if (!fileName.startsWith("config")) {
                resource = classLoader.getResource("config/" + fileName);
                if (resource != null) {
                    return new File(resource.getFile());
                }
            }

        }
        return null;
    }

    public static InputStream findInputStream(String fileName) {
        return findInputStream(fileName, Thread.currentThread().getContextClassLoader());
    }

    public static InputStream findInputStream(String fileName, ClassLoader classLoader) {
        try {
            fileName = fileName.replace("\\", "/");

            File file = new File(fileName);
            if (exists(file) && file.isFile()) {
                return new FileInputStream(file);
            }

            if (!fileName.startsWith("config")) {
                file = new File("config/" + fileName);
                if (exists(file) && file.isFile()) {
                    return new FileInputStream(file);
                }
            }

            {
                if (fileName.startsWith("/")) {
                    fileName = fileName.substring(1);
                }
                /*todo 获取resource 不能是/开始的*/
                InputStream resource = classLoader.getResourceAsStream(fileName);
                if (resource != null) {
                    return resource;
                }

                if (!fileName.startsWith("config")) {
                    resource = classLoader.getResourceAsStream("config/" + fileName);
                    if (resource != null) {
                        return resource;
                    }
                }

            }
            return null;
        } catch (Exception e) {
            throw Throw.as(e);
        }
    }

    public static void resourceStream(String path, ConsumerE2<String, InputStream> call) {
        resourceStream(Thread.currentThread().getContextClassLoader(), path, call);
    }

    public static void resourceStream(ClassLoader classLoader, final String path, ConsumerE2<String, InputStream> call) {
        try {
            URL resource = classLoader.getResource(path);
            String findPath = URLDecoder.decode(resource.getPath(), StandardCharsets.UTF_8);
            if (findPath.contains(".zip!") || findPath.contains(".jar!")) {
                findPath = findPath.substring(5, findPath.indexOf("!/"));
                try (ReadZipFile zipFile = new ReadZipFile(findPath)) {
                    zipFile.forEachStream((name, inputStream) -> {
                        if (name.startsWith(path)) {
                            call.accept(name, inputStream);
                        }
                    });
                }
            } else {
                File file = new File(findPath);
                if (file.exists() && file.isDirectory()) {
                    File[] files = file.listFiles();
                    for (File file1 : files) {
                        if (!file1.isDirectory()) {
                            try (FileInputStream fileInputStream = new FileInputStream(file1)) {
                                call.accept(file1.getName(), fileInputStream);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw Throw.as("resources:" + path, e);
        }
    }

    public static File file(String fileName) {
        return new File(fileName);
    }

    public static File file(URI uri) {
        return new File(uri);
    }

    /** 获取文件名 */
    public static String fileName(File file) {
        String fileName = file.getName();
        int indexOf = fileName.indexOf(".");
        return fileName.substring(0, indexOf);
    }

    /** 获取扩展名 */
    public static String extendName(File file) {
        return extendName(file.getName());
    }

    /** 获取扩展名 */
    public static String extendName(String fileName) {
        int indexOf = fileName.indexOf("?");
        if (indexOf >= 0) {
            fileName = fileName.substring(0, indexOf);
        }
        fileName = fileName.substring(fileName.lastIndexOf(".") + 1);
        return fileName;
    }

    public static boolean exists(String fileName) {
        return file(fileName).exists();
    }

    public static boolean exists(File file) {
        return file.exists();
    }

    /** 下载网络资源 */
    public static void downloadFile(String url, String saveFileName) {
        try {
            long millis = System.currentTimeMillis();
            URL url1 = new URL(url);
            try (InputStream inputStream = url1.openStream()) {
                File file = new File(saveFileName);
                FileWriteUtil.fileOutputStream(
                        file,
                        false,
                        outputStream -> FileReadUtil.readBytes(outputStream, inputStream)
                );
                long costTime = System.currentTimeMillis() - millis;
                log().info("下载文件：{}, 保存：{}, 大小：{} kb, 耗时：{} ms", url, saveFileName, file.length() / 1024f, costTime);
            }
        } catch (Exception e) {
            throw Throw.as(url, e);
        }
    }

    public static void ftl2File(ClassLoader classLoader, String dir, String ftl, Map<String, Object> obj, String outFile) {
        TemplatePack.build(classLoader, dir).ftl2File(ftl, obj, outFile);
    }

    public static String ftl2String(ClassLoader classLoader, String dir, String ftl, Map<String, Object> obj) {
        return TemplatePack.build(classLoader, dir).ftl2String(ftl, obj);
    }

    public static byte[] ftl2Bytes(ClassLoader classLoader, String dir, String ftl, Map<String, Object> obj) {
        return TemplatePack.build(classLoader, dir).ftl2Bytes(ftl, obj);
    }

    public static File createFile(String fileName) {
        return createFile(file(fileName));
    }

    public static File createFile(File file) {
        return createFile(file, false);
    }

    public static File createFile(File file, boolean fugai) {
        try {
            if (!fugai) {
                if (file.exists()) {
                    /*如果文件已经存在，无需创建*/
                    return file;
                }
            }
            mkdirs(file);
            file.createNewFile();
            return file;
        } catch (Exception e) {
            throw Throw.as(file.getPath(), e);
        }
    }

    /**
     * 删除文件 或者 文件夹
     *
     * @param fileName
     */
    public static void del(String fileName) {
        del(file(fileName));
    }

    /**
     * 删除文件 或者 文件夹
     *
     * @param file
     */
    public static void del(File file) {
        if (file.isDirectory()) {
            final File[] listFiles = file.listFiles();
            if (listFiles != null && listFiles.length > 0) {
                for (File listFile : listFiles) {
                    del(listFile);
                }
            }
        }

        final boolean delete = file.delete();

        log().info("删除{}：{}, {}", file.isFile() ? "文件" : "文件夹", file.getName(), delete);
    }

    /**
     * 创建文件夹
     *
     * @param fileName 需要创建文件夹的名字
     */
    public static void mkdirs(String fileName) {
        mkdirs(file(fileName));
    }

    /**
     * 创建文件夹
     *
     * @param file 需要创建文件夹
     */
    public static void mkdirs(File file) {
        final File absoluteFile = file.getAbsoluteFile();
        if (absoluteFile.isDirectory()) {
            absoluteFile.mkdirs();
        } else {
            absoluteFile.getParentFile().mkdirs();
        }
    }

    /**
     * 获取所有的目录
     */
    public static Collection<File> dirs(String path) {
        return dirs(file(path));
    }

    /**
     * 获取所有的目录
     */
    public static Collection<File> dirs(File path) {
        List<File> files = new ArrayList<>();
        dirs(files, path);
        return files;
    }

    /**
     * 获取所有的目录
     */
    public static void dirs(List<File> files, File path) {
        final File[] listFiles = path.listFiles();
        if (listFiles != null && listFiles.length > 0) {
            for (File file : listFiles) {
                if (file.isDirectory()) {
                    files.add(file);
                }
            }
        }
    }

    /**
     * 获取所有的目录，包含子目录
     */
    public static Collection<File> loopDirs(String path) {
        return loopDirs(file(path));
    }

    /**
     * 获取所有的目录，包含子目录
     */
    public static Collection<File> loopDirs(File path) {
        List<File> files = new ArrayList<>();
        final File[] listFiles = path.listFiles();
        if (listFiles != null && listFiles.length > 0) {
            for (File file : listFiles) {
                if (file.isDirectory()) {
                    files.add(file);
                    dirs(files, file);
                }
            }
        }
        return files;
    }

    /**
     * 查找当前文件夹,所有文件
     *
     * @param path    文件或者文件夹
     * @param suffixs 后缀扩展
     * @return
     * @throws Exception
     */
    public static Collection<File> lists(String path, String... suffixs) {
        return lists(file(path), suffixs);
    }

    /**
     * 查找当前文件夹,所有文件
     *
     * @param file    文件或者文件夹
     * @param suffixs 后缀扩展
     * @return
     * @throws Exception
     */
    public static Collection<File> lists(File file, String... suffixs) {
        List<File> files = new ArrayList<>();
        findFile(file, false, files::add, suffixs);
        return files;
    }

    /**
     * 查找当前文件夹，包含子文件夹,所有文件
     *
     * @param path    文件或者文件夹
     * @param suffixs 后缀扩展
     * @throws Exception
     */
    public static Collection<File> loopLists(String path, String... suffixs) {
        return loopLists(file(path), suffixs);
    }

    /**
     * 查找当前文件夹，包含子文件夹,所有文件
     *
     * @param file    文件或者文件夹
     * @param suffixs 后缀扩展
     */
    public static Collection<File> loopLists(File file, String... suffixs) {
        return findFile(file, true, suffixs);
    }

    /**
     * 查找当前文件夹，包含子文件夹,所有文件
     *
     * @param file    文件或者文件夹
     * @param loop    是否查找子文件夹
     * @param suffixs 后缀扩展
     */
    public static Collection<File> findFile(File file, boolean loop, String... suffixs) {
        List<File> files = new ArrayList<>();
        findFile(file, loop, files::add, suffixs);
        return files;
    }

    /**
     * 查找当前文件夹，包含子文件夹,所有文件
     *
     * @param file           文件或者文件夹
     * @param loop           是否查找子文件夹
     * @param fileConsumerE1 回调
     * @param suffixs        后缀扩展
     */
    public static void findFile(File file, boolean loop, ConsumerE1<File> fileConsumerE1, String... suffixs) {
        try {
            if (file.isFile()) {
                if (suffixs != null && suffixs.length > 0) {
                    boolean check = false;
                    for (String suffix : suffixs) {
                        if (file.getName().endsWith(suffix)) {
                            check = true;
                            break;
                        }
                    }
                    if (!check) {
                        return;
                    }
                }
                fileConsumerE1.accept(file);
            } else if (file.isDirectory()) {
                final File[] listFiles = file.listFiles();
                if (listFiles != null && listFiles.length > 0) {
                    for (File listFile : listFiles) {
                        if (listFile.isFile()) {
                            findFile(listFile, loop, fileConsumerE1, suffixs);
                        }
                    }
                    if (loop) {
                        for (File listFile : listFiles) {
                            if (listFile.isDirectory()) {
                                findFile(listFile, loop, fileConsumerE1, suffixs);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw Throw.as(e);
        }
    }

    public static File findFile(File file, boolean loop, FunctionE<File, Boolean> function) {
        try {
            if (file.isFile()) {
                if (function.apply(file)) {
                    return file;
                }
            } else if (file.isDirectory()) {
                final File[] listFiles = file.listFiles();
                if (listFiles != null && listFiles.length > 0) {
                    for (File listFile : listFiles) {
                        if (listFile.isFile()) {
                            if (function.apply(listFile)) {
                                return listFile;
                            }
                        }
                    }
                    if (loop) {
                        for (File listFile : listFiles) {
                            if (listFile.isDirectory()) {
                                final File findFile = findFile(listFile, loop, function);
                                if (findFile != null) {
                                    return findFile;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw Throw.as(e);
        }
        return null;
    }

}
