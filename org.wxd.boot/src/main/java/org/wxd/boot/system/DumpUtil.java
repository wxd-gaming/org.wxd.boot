package org.wxd.boot.system;

import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.format.ByteFormat;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-12-30 20:33
 */
@Slf4j
public class DumpUtil {

    public static final DecimalFormat df2 = new DecimalFormat("0.00%");

    public static ByteFormat.FormatInfo ByteFormatInfo = ByteFormat.FormatInfo.MB;

    public static File file = new File(JvmUtil.userHome());

    public static long WarnSize = 500L * 1014 * 1024;

    public static String Xmx = "";
    public static String HomeMx = "";

    public static void main(String[] args) {
        System.out.println(dMonitor());
    }

    /**
     * 执行垃圾回收
     * <p>垃圾回收是jvm说了算，只是调用了，至于什么时候执行管不了
     */
    public static void gc() {
        System.gc();
    }

    /** 添加程序监控，磁盘和内存 */
    public static final String dMonitor() {
        StringBuilder stringBuilder = new StringBuilder();
        freeMemory(stringBuilder);
        stringBuilder.append("\n");
        DumpUtil.checkDiskInfo(stringBuilder, file);
        return stringBuilder.toString();
    }

    /** 获取当前空闲内存 */
    public static long freeMemory() {
        return freeMemory(null);
    }

    /** 获取当前空闲内存 */
    public static long freeMemory(StringBuilder stringBuilder) {
        ByteFormat byteFormat = new ByteFormat();
        final Runtime runtime = Runtime.getRuntime();
        final long maxMemory = runtime.maxMemory();
        final long totalMemory = runtime.totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        freeMemory = (maxMemory - totalMemory + freeMemory);
        long usedMemory = maxMemory - freeMemory;
        float warn = 1f * usedMemory / maxMemory;
        if (stringBuilder != null) {
            stringBuilder.append("内存空间 -> ");
            stringBuilder.append("总量：");
            byteFormat.addFlow(maxMemory);
            final String mx = byteFormat.toString(ByteFormatInfo);
            stringBuilder.append(mx).append(", ");
            byteFormat.clear();
            stringBuilder.append("空闲：");
            byteFormat.addFlow(freeMemory);
            byteFormat.toString(ByteFormatInfo, stringBuilder);
            stringBuilder.append(", ");
            byteFormat.clear();
            stringBuilder.append("使用：");
            byteFormat.addFlow(usedMemory);
            final String use = byteFormat.toString(ByteFormatInfo);
            stringBuilder.append(use).append(", ");
            byteFormat.clear();

            stringBuilder.append("占比：").append(df2.format(warn));

            Xmx = "分配=" + mx + ", 使用=" + use;
        }
        return freeMemory;
    }

    /** 指定目录的空间 剩余 MB 返回 true 表示超过警告阈值 */
    public static boolean checkDiskInfo(String path) {
        return checkDiskInfo(null, new File(path));
    }

    /** 指定目录的空间 剩余 MB 返回 true 表示超过警告阈值 */
    public static boolean checkDiskInfo(StringBuilder stringBuilder, File file) {

        if (!file.exists()) {
            try {
                file.mkdirs();
                file.createNewFile();
            } catch (Exception e) {
                log.error(file.toString(), e);
            }
        }

        final long usedSpace = file.getTotalSpace() - file.getFreeSpace();
        final float warn = 1f * usedSpace / file.getTotalSpace();

        if (stringBuilder != null) {
            ByteFormat byteFormat = new ByteFormat();
            stringBuilder.append("磁盘空间 -> ");
            stringBuilder.append("总量：");
            final String mx = byteFormat.addFlow(file.getTotalSpace()).toString(ByteFormatInfo);
            stringBuilder.append(mx).append(", ");

            byteFormat.clear();
            stringBuilder.append("空闲：");
            byteFormat.addFlow(file.getFreeSpace()).toString(ByteFormatInfo, stringBuilder);
            stringBuilder.append(", ");

            byteFormat.clear();

            stringBuilder.append("使用：");
            final String use = byteFormat.addFlow(usedSpace).toString(ByteFormatInfo);
            stringBuilder.append(use).append(", ");
            /*使用空间*/

            stringBuilder.append("占比：").append(df2.format(warn));
            HomeMx = "总共=" + mx + ", 使用=" + use;
        }

        return file.getFreeSpace() < WarnSize;
    }

    /** 获取所有线程 */
    public static String threadInfos() {
        StringBuilder stringBuilder = new StringBuilder();
        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        final int threadCount = threadMXBean.getThreadCount();
        stringBuilder.append("Total Number of threads " + threadCount).append("\n");
        Arrays.stream(threadMXBean.dumpAllThreads(true, true))
                .sorted(Comparator.comparing(ThreadInfo::getThreadName))
                .forEach(threadInfo -> {
                    stringBuilder.append("\n").append("\n");
                    stringBuilder.append(threadInfo.toString()).append("\n");
                });
        return stringBuilder.toString();
    }

    /** 获取当前线程组所有线程 */
    public Map<Long, Thread> threads() {
        return threads(Thread.currentThread().getThreadGroup());
    }

    /** 获取当前线程组所有线程 */
    public Map<Long, Thread> threads(ThreadGroup currentGroup) {
        Map<Long, Thread> threads = new TreeMap<>();
        threads(threads, currentGroup);
        return threads;
    }

    public void threads(Map<Long, Thread> ths, ThreadGroup currentGroup) {
        int noThreads = currentGroup.activeCount();
        Thread[] lstThreads = new Thread[noThreads];
        currentGroup.enumerate(lstThreads);
        for (Thread lstThread : lstThreads) {
            ths.put(lstThread.getId(), lstThread);
        }
        if (currentGroup.getParent() != null) {
            threads(ths, currentGroup.getParent());
        }
    }


}
