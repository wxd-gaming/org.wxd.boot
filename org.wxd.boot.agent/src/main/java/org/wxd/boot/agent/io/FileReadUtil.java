package org.wxd.boot.agent.io;


import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.agent.function.Consumer2;
import org.wxd.boot.agent.function.ConsumerE1;
import org.wxd.boot.agent.lang.Record2;
import org.wxd.boot.agent.zip.ReadZipFile;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件读取
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-08-18 14:41
 **/
public class FileReadUtil implements Serializable {


    /** 递归查找所有文件 */
    public static Map<String, byte[]> readBytesAll(File file, String... extendNames) {
        return readBytesStream(file, extendNames).collect(Collectors.toMap(Record2::t1, Record2::t2));
    }

    /** 递归查找所有文件 */
    public static Stream<Record2<String, byte[]>> readBytesStream(File file, String... extendNames) {
        return FileUtil.walkFiles(file.getPath(), extendNames)
                .map(f -> new Record2<>(f.getPath(), readBytes(f)));
    }

    /** 递归查找所有文件 */
    public static Map<String, byte[]> readBytesAll(String file, String... extendNames) {
        return readBytesAll(FileUtil.findFile(file), extendNames);
    }

    /** 递归查找所有文件 */
    public static void readBytesAll(File file, String[] extendNames, Consumer2<String, byte[]> call) {
        FileUtil.walkFiles(file, extendNames)
                .forEach(f -> call.accept(f.getPath(), readBytes(f)));
    }

    public static String readString(String fileName) {
        return readString(fileName, StandardCharsets.UTF_8);
    }

    /** 获取jar包内资源 需要传入classloader */
    public static String readString(ClassLoader classLoader, String fileName) {
        return readString(classLoader, fileName, StandardCharsets.UTF_8);
    }

    public static String readString(String fileName, Charset charset) {
        return readString(Thread.currentThread().getContextClassLoader(), fileName, charset);
    }

    /** 获取jar包内资源 需要传入classloader */
    public static String readString(ClassLoader classLoader, String fileName, Charset charset) {
        Record2<String, InputStream> inputStream = FileUtil.findInputStream(classLoader, fileName);
        if (inputStream == null) {
            System.out.printf("文件 %s 查找失败\n", fileName);
            return null;
        }
        return readString(inputStream.t2(), charset);
    }

    public static String readString(File file) {
        return readString(file, StandardCharsets.UTF_8);
    }

    public static String readString(File file, Charset charset) {
        try (final FileInputStream fileInputStream = new FileInputStream(file)) {
            return readString(fileInputStream, charset);
        } catch (Exception e) {
            throw Throw.as(e);
        }
    }

    public static String readString(InputStream inputStream) {
        return readString(inputStream, StandardCharsets.UTF_8);
    }

    public static String readString(InputStream inputStream, Charset charset) {
        byte[] readBytes = readBytes(inputStream);
        return new String(readBytes, charset);
    }

    public static List<String> readLines(String fileName) {
        return readLines(fileName, StandardCharsets.UTF_8);
    }

    public static List<String> readLines(String fileName, Charset charset) {
        Record2<String, InputStream> inputStream = FileUtil.findInputStream(fileName);
        if (inputStream == null) {
            System.out.printf("文件 %s 查找失败\n", fileName);
            return null;
        }
        return readLines(inputStream.t2(), charset);
    }

    public static List<String> readLines(InputStream fileInputStream, Charset charset) {
        List<String> lines = new ArrayList<>();
        readLine(fileInputStream, charset, lines::add);
        return lines;
    }

    public static List<String> readLines(File file) {
        return readLines(file, StandardCharsets.UTF_8);
    }

    public static List<String> readLines(File file, Charset charset) {
        List<String> lines = new ArrayList<>();
        readLine(file, charset, lines::add);
        return lines;
    }

    public static void readLine(File file, Charset charset, ConsumerE1<String> call) {
        try (final FileInputStream fileInputStream = new FileInputStream(file)) {
            readLine(fileInputStream, charset, call);
        } catch (Exception e) {
            throw Throw.as(e);
        }
    }

    public static void readLine(InputStream fileInputStream, Charset charset, ConsumerE1<String> call) {
        try (final Scanner sc = new Scanner(fileInputStream, charset)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line == null)
                    break;
                call.accept(line);
            }
        } catch (Exception e) {
            throw Throw.as(e);
        }
    }

    public static byte[] readBytes(String file) {
        return readBytes(FileUtil.findFile(file));
    }

    public static byte[] readBytes(File file) {
        try (final FileInputStream fileInputStream = new FileInputStream(file)) {
            return readBytes(fileInputStream);
        } catch (Exception e) {
            throw Throw.as(e);
        }
    }

    public static byte[] readBytes(InputStream inputStream) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            readBytes(outputStream, inputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw Throw.as(e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                throw Throw.as(e);
            }
        }
    }

    public static void readBytes(OutputStream outputStream, InputStream inputStream) {
        byte[] bytes = new byte[200];
        try {
            int read = 0;
            while ((read = inputStream.read(bytes, 0, bytes.length)) >= 0) {
                outputStream.write(bytes, 0, read);
            }
        } catch (Exception e) {
            throw Throw.as(e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                throw Throw.as(e);
            }
        }
    }

    public static void readJarClass(HashMap<String, byte[]> maps, String jarPath, String... names) {
        try (ReadZipFile readZipFile = new ReadZipFile(jarPath)) {
            readZipFile.forEach((fileName, bytes) -> {
                String lowerCase = fileName.toLowerCase();
                for (String name : names) {
                    if (lowerCase.contains(name.toLowerCase())) {
                        String replace = fileName.replace("/", ".").replace("\\", ".");
                        maps.put(replace, bytes);
                        break;
                    }
                }
            });
        }
    }

}
