package org.wxd.boot.assist;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 监控日志
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2024-01-06 14:16
 **/
public class MonitorRecord {

    static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS");

    long startTime = 0;
    /** 结束时间 */
    float execMs = 0;
    StackTraceElement startStack;
    List<StackRecord> recordList = new ArrayList<>();
    String head;

    public MonitorRecord() {
        this(3);
    }

    public MonitorRecord(int stackIndex) {
        this.start(stackIndex + 1);
    }

    public void start() {
        this.start(3);
    }

    public void start(int stackIndex) {
        startTime = System.nanoTime();
        execMs = 0;
        startStack = Thread.currentThread().getStackTrace()[stackIndex];
        head = Thread.currentThread().toString() + " " + startStack.getClassName() + "." + startStack.getMethodName() + ":" + startStack.getLineNumber();
    }

    public void monitor(StackTraceElement[] sts, String str, float ms) {
        //stringBuilder.append("|");
        List<String> strings = new ArrayList<>();
        if (sts != null && sts.length > 0) {
            for (StackTraceElement st : sts) {
                strings.add(st.getClassName() + "." + st.getMethodName() + ":" + st.getLineNumber());
                //stringBuilder.append("_");
                if (startStack.getClassName().equals(st.getClassName())
                        && startStack.getMethodName().equals(st.getMethodName())) {
                    break;
                }
            }
            Collections.reverse(strings);
        }

        StackRecord stack = findStack(strings);
        Iterator<Map.Entry<String, Float>> iterator = stack.stacks.entrySet().iterator();
        Map.Entry<String, Float> next = null;
        for (int i = 0; i < strings.size(); i++) {
            if (iterator.hasNext()) {
                next = iterator.next();
            }
        }
        next.setValue(ms);
        //stringBuilder.append(str).append(", cost：").append(ms).append(" ms").append("\n");
    }

    public StackRecord findStack(List<String> strings) {
        for (StackRecord stackRecord : recordList) {
            ext:
            {
                int index = 0;
                for (String key : stackRecord.stacks.keySet()) {
                    if (index < strings.size() - 1) {
                        if (!key.equals(strings.get(index))) {
                            break ext;
                        }
                    }
                    index++;
                }
                return stackRecord;
            }
        }
        StackRecord stackRecord = new StackRecord();
        for (String string : strings) {
            stackRecord.stacks.put(string, 0f);
        }
        recordList.add(stackRecord);
        return stackRecord;
    }

    public void over() {
        if (execMs == 0) {
            execMs = ((System.nanoTime() - startTime) / 10000 / 100f);
        }
    }

    /** 获取执行时间 */
    public float execMs() {
        over();
        return execMs;
    }

    public void print() {
        System.out.println(toString());
    }

    @Override public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(simpleDateFormat.format(new Date())).append(" ").append(head).append("\n");
        for (StackRecord stackRecord : recordList) {
            int line = 0;
            for (Map.Entry<String, Float> entry : stackRecord.stacks.entrySet()) {
                stringBuilder.append("|");
                for (int i = 0; i < line; i++) {
                    stringBuilder.append("_");
                }
                stringBuilder.append(entry.getKey());
                if (entry.getValue() > 0)
                    stringBuilder.append(" 耗时：").append(entry.getValue()).append(" ms");
                stringBuilder.append("\n");
                line++;
            }
        }
        stringBuilder
                .append(startStack.getClassName() + "." + startStack.getMethodName())
                .append("耗时：").append(execMs()).append("ms");
        return stringBuilder.toString();
    }

    public static class StackRecord {
        LinkedHashMap<String, Float> stacks = new LinkedHashMap<>();
    }

}
