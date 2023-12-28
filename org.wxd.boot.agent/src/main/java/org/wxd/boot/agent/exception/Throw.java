package org.wxd.boot.agent.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-10-29 10:16
 **/
public class Throw extends RuntimeException implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(Throw.class);
    public static final StackTraceElement[] StackTraceEmpty = new StackTraceElement[0];

    public static RuntimeException as(Throwable throwable) {
        return as(null, throwable);
    }

    public static RuntimeException as(String msg, Throwable throwable) {

        String message = null;

        if (msg != null && !msg.isEmpty() && !msg.isBlank()) {
            message = msg;
        }

        String throwableMessage;
        if (throwable instanceof Throw) {
            throwableMessage = throwable.getLocalizedMessage();
        } else {
            throwableMessage = throwable.toString();
        }

        if (throwableMessage != null && !throwableMessage.isEmpty() && !throwableMessage.isBlank()) {
            if (message != null) message = message + ", " + throwableMessage;
            else message = throwableMessage;
        }

        return new Throw(message, throwable);
    }

    public static String ofString(Throwable throwable) {
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
    public static void ofString(StringBuilder stringBuilder, Throwable throwable) {
        if (throwable != null) {

            ofString(stringBuilder, throwable.getCause());

            stringBuilder.append("\n");

            if (!Throw.class.equals(throwable.getClass())) {
                stringBuilder.append(throwable.getClass().getName());
            } else {
                stringBuilder.append(RuntimeException.class.getName());
            }
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

    public static void ofString(StringBuilder stringBuilder, StackTraceElement[] stackTraces) {
        for (StackTraceElement e : stackTraces) {
            stringBuilder.append("    at ");
            ofString(stringBuilder, e);
            stringBuilder.append("\n");
        }
    }

    public static String ofString(StackTraceElement traceElement) {
        StringBuilder stringBuilder = new StringBuilder();
        ofString(stringBuilder, traceElement);
        return stringBuilder.toString();
    }

    public static void ofString(StringBuilder stringBuilder, StackTraceElement traceElement) {
        stringBuilder.append(traceElement.getClassName()).append(".").append(traceElement.getMethodName())
                .append("(").append(traceElement.getFileName()).append(":").append(traceElement.getLineNumber()).append(")");
    }

    /**
     * 代理的异常过滤掉
     *
     * @param throwable
     * @return
     */
    public static Throwable filterInvoke(Throwable throwable) {
        if (throwable instanceof InvocationTargetException) {
            if (throwable.getCause() != null) {
                return filterInvoke(throwable.getCause());
            }
        }
        return throwable;
    }

    /**
     * 判断异常是否是线程终止异常，如果是继续终止线程
     *
     * @param throwable
     * @return
     */
    public static boolean isInterruptedException(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        if (throwable instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return true;
        }
        return isInterruptedException(throwable.getCause());
    }

    public static StackTraceElement[] revertStackTrace(StackTraceElement[] sts) {
        if (sts != null && sts.length > 0) {
            int len = sts.length;
            if (len > 3) {
                len = 3;
            }
            StackTraceElement[] stackTraces = new StackTraceElement[len];
            System.arraycopy(sts, 0, stackTraces, 0, len);
            return stackTraces;
        }
        return StackTraceEmpty;
    }

    static String message(String msg, Throwable throwable) {
        String throwableName;
        if (throwable.getClass().equals(Throw.class)) {
            throwableName = throwable.getMessage();
        } else {
            throwableName = throwable.getClass().getName() + ", " + throwable.getMessage();
        }
        if (!(msg == null || msg.isBlank())) {
            throwableName += ", " + msg;
        }
        return throwableName;
    }

    public Throw(String message) {
        super(message);
    }

    public Throw(String message, Throwable throwable) {
        super(message, throwable.getCause(), false, true);
        this.setStackTrace(throwable.getStackTrace());
    }

    @Override public String toString() {
        String s = getClass().getName();
        String message = getLocalizedMessage();
        return (message != null) ? (s + ": " + message) : s;
    }
}
