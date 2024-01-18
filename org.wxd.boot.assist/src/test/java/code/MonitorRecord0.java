package code;

import lombok.Getter;
import lombok.Setter;
import org.wxd.boot.assist.AssistMonitor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 监控日志
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2024-01-06 14:16
 **/
@Getter
public class MonitorRecord0 {

    long startTime = 0;
    /** 结束时间 */
    float execMs = 0;
    StackTraceElement startStack;
    String head;
    List<Stack> stacks = new ArrayList<>();

    public MonitorRecord0() {
        this(3);
    }

    public MonitorRecord0(int stackIndex) {
        this.start(stackIndex + 1);
    }

    public String string() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Stack stack : getStacks()) {
            stack.toString(stringBuilder, 1);
        }
        return stringBuilder.toString();
    }

    public void start() {
        this.start(3);
    }

    public void start(int stackIndex) {
        startTime = System.nanoTime();
        execMs = 0;
        startStack = Thread.currentThread().getStackTrace()[stackIndex];
        head = "[" + AssistMonitor.SIMPLE_DATE_FORMAT.format(new Date()) + "]"
                + " [" + Thread.currentThread().toString() + "]"
                + " - 文件：" + startStack.getFileName()
                + ", 方法：" + startStack.getClassName() + "." + startStack.getMethodName()
                + "\n堆栈：";
    }

    public void add(StackTraceElement[] es, float execMs) {
        StackTraceElement[] elements = rever(es);
        Stack findStack = null;
        List<Stack> tmpStacks = stacks;
        int ei = 0;
        for (; ei < elements.length; ei++) {
            StackTraceElement element = elements[ei];
            for (Stack tmpStack : tmpStacks) {
                if (tmpStack.check(element)) {
                    findStack = tmpStack;
                    tmpStacks = tmpStack.getSubs();
                }
            }
        }
        if (findStack == null) {
            findStack = new Stack();
            stacks.add(findStack);
            ei = 0;
        }
        for (; ei < elements.length; ei++) {
            StackTraceElement element = elements[ei];
            findStack.setStackTraceElement(element);
            if (ei < elements.length - 1) {
                Stack sub = new Stack();
                findStack.getSubs().add(sub);
                findStack = sub;
            }
        }
        findStack.setExecMs(execMs);
    }

    static StackTraceElement[] rever(StackTraceElement[] ts) {
        StackTraceElement[] rets = new StackTraceElement[ts.length];
        for (int t = 0; t < ts.length; t++) {
            rets[t] = ts[ts.length - 1 - t];
        }
        return rets;
    }

    @Getter
    @Setter
    public class Stack {

        protected StackTraceElement stackTraceElement;
        protected List<Stack> subs = new ArrayList<>();
        protected float execMs;

        public boolean check(StackTraceElement element) {
            if (element.getClassName().equals(stackTraceElement.getClassName())
                    && element.getMethodName().equals(stackTraceElement.getMethodName())
                    && element.getLineNumber() == stackTraceElement.getLineNumber()) {
                return true;
            }
            return false;
        }

        public void toString(StringBuilder stringBuilder, int flow) {
            int i1 = flow - 1;
            stringBuilder.append("|");
            for (int i = 0; i < i1; i++) {
                stringBuilder.append("_");
            }
            stringBuilder.append(stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName() + "():" + stackTraceElement.getLineNumber())
                    .append(", 耗时：").append(getExecMs()).append(" ms")
                    .append("\n");
            for (Stack sub : subs) {
                sub.toString(stringBuilder, flow + 1);
            }
        }

    }

}
