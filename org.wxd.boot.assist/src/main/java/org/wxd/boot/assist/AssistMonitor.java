package org.wxd.boot.assist;

import java.io.File;
import java.io.OutputStream;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

/**
 * 动态探针初始化类
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2024-01-05 21:03
 **/
public class AssistMonitor {

    public static final String ASSIST_OUT_DIR = "target/assist-out";
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS");
    public static final InheritableThreadLocal<MonitorRecord> THREAD_LOCAL = new InheritableThreadLocal<>();
    public static AssistClassTransform transformer = null;
    public static OpenOption[] openOptions;
    public static Path assist_path_error;
    public static Path assist_path_log;

    public static void premain(String ages, Instrumentation instrumentation) {
        try {
            File file = new File(AssistMonitor.ASSIST_OUT_DIR);
            if (Files.exists(file.toPath())) {
                Files.walk(file.toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            file.mkdirs();
            openOptions = new OpenOption[2];
            /*追加文本,如果文件不存在则创建*/
            openOptions[0] = StandardOpenOption.CREATE;
            openOptions[1] = StandardOpenOption.APPEND;

            assist_path_log = new File(AssistMonitor.ASSIST_OUT_DIR + "/assist.log").toPath();
            assist_path_error = new File(AssistMonitor.ASSIST_OUT_DIR + "/assist.error").toPath();
            printLog("[" + AssistMonitor.SIMPLE_DATE_FORMAT.format(new Date()) + "] 初始化完成 args " + ages);

        } catch (Throwable e) {
            e.printStackTrace(System.err);
            printError("初始化", e);
        }
        transformer = new AssistClassTransform(ages);
        instrumentation.addTransformer(transformer);
    }

    //如果没有实现上面的方法，JVM将尝试调用该方法
    public static void premain(String agentArgs) {
    }

    public static void printLog(String info) {
        print(assist_path_log, info);
    }

    public static void printError(String info, Throwable throwable) {
        printError(info + " - " + ofString(throwable));
    }

    public static void printError(String info) {
        print(assist_path_error, info);
    }

    public static void print(Path path, String info) {
        try (OutputStream out = Files.newOutputStream(path, openOptions)) {
            out.write("\n----------------------------------start--------------------------------------\n".getBytes(StandardCharsets.UTF_8));
            out.write(info.getBytes(StandardCharsets.UTF_8));
            out.write("\n-----------------------------------end-------------------------------------\n".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static MonitorRecord.MonitorStack start() {
        MonitorRecord monitorRecord = THREAD_LOCAL.get();
        if (monitorRecord == null) {
            monitorRecord = new MonitorRecord(3);
            THREAD_LOCAL.set(monitorRecord);
            return new MonitorRecord.MonitorStack(monitorRecord.startTime, false);
        } else {
            return new MonitorRecord.MonitorStack(true);
        }
    }

    /** 是否有父级状态 */
    public static void close(MonitorRecord.MonitorStack monitorStack, IAssistMonitor iAssistMonitor) {
        MonitorRecord monitorRecord = THREAD_LOCAL.get();
        if (monitorRecord == null) return;
        if (monitorStack.isHasParent()) {
            float ms = (System.nanoTime() - monitorStack.getStartTime()) / 10000 / 100f;
            if (ms > 1)
                monitorRecord.monitor(stacks(3), ms);
            return;
        }
        monitorRecord.over();
        THREAD_LOCAL.remove();
        if (monitorRecord.execMs() > iAssistMonitor.waringTime()) {
            iAssistMonitor.print(monitorRecord);
        }
    }

    public static void close(MonitorRecord.MonitorStack monitorStack) {
        MonitorRecord monitorRecord = THREAD_LOCAL.get();
        if (monitorRecord == null) return;
        if (monitorStack.isHasParent()) {
            float ms = (System.nanoTime() - monitorStack.getStartTime()) / 10000 / 100f;
            if (ms > 1)
                monitorRecord.monitor(stacks(3), ms);
            return;
        }
        monitorRecord.over();
        THREAD_LOCAL.remove();
        if (monitorRecord.execMs() > 33) {
            AssistMonitor.printLog(monitorRecord.toString());
        }
    }

    public static StackTraceElement[] stacks(int index) {
        StackTraceElement[] sts = Thread.currentThread().getStackTrace();
        StackTraceElement[] tmp = new StackTraceElement[sts.length - index];
        System.arraycopy(sts, index, tmp, 0, tmp.length);
        return tmp;
    }


    static String ofString(Throwable throwable) {
        StringBuilder stringBuilder = new StringBuilder();
        ofString(stringBuilder, throwable);
        return stringBuilder.toString();
    }

    /**
     * 处理错误日志的堆栈信息
     *
     * @param stringBuilder
     * @param throwable
     */
    static void ofString(StringBuilder stringBuilder, Throwable throwable) {
        if (throwable != null) {
            ofString(stringBuilder, throwable.getCause());
            stringBuilder.append("\n");
            stringBuilder.append(throwable.getClass().getName());
            stringBuilder.append(": ");
            if (throwable.getMessage() != null && !throwable.getMessage().isEmpty()) {
                stringBuilder.append(throwable.getMessage());
            } else {
                stringBuilder.append("null");
            }
            stringBuilder.append("\n");
            StackTraceElement[] stackTraces = throwable.getStackTrace();
            ofString(stringBuilder, stackTraces);
            stringBuilder.append("-----------------------------------------------------------------------------");
        }
    }

    static void ofString(StringBuilder stringBuilder, StackTraceElement[] stackTraces) {
        for (StackTraceElement e : stackTraces) {
            stringBuilder.append("    at ");
            ofString(stringBuilder, e);
            stringBuilder.append("\n");
        }
    }

    static String ofString(StackTraceElement traceElement) {
        StringBuilder stringBuilder = new StringBuilder();
        ofString(stringBuilder, traceElement);
        return stringBuilder.toString();
    }

    static void ofString(StringBuilder stringBuilder, StackTraceElement traceElement) {
        stringBuilder.append(traceElement.getClassName()).append(".").append(traceElement.getMethodName())
                .append("(").append(traceElement.getFileName()).append(":").append(traceElement.getLineNumber()).append(")");
    }


}
