package wxdgaming.boot.core.threading;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot.core.collection.ObjMap;

import java.util.Map;

/**
 * 本地线程变量
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2024-04-24 20:26
 **/
@Slf4j
@Getter
public class ThreadContext extends ObjMap {

    private static final ThreadLocal<ThreadContext> local = new InheritableThreadLocal<>();

    /** 获取参数 */
    public static <T> T context(final Class<T> clazz) {
        return (T) context().get(clazz.getName());
    }

    /** 获取参数 */
    public static <T> T context(final Object name) {
        return (T) context().get(name);
    }

    /** put参数 */
    public static <T> T putContent(final Class<T> clazz) {
        try {
            T ins = clazz.getDeclaredConstructor().newInstance();
            putContentIfAbsent(ins);
            return ins;
        } catch (Exception e) {
            throw new RuntimeException(clazz.getName(), e);
        }
    }

    /** put参数 */
    public static <T> void putContent(final T ins) {
        context().put(ins.getClass().getName(), ins);
    }

    /** put参数 */
    public static <T> void putContent(final Object name, T ins) {
        context().put(name, ins);
    }

    /** put参数 */
    public static <T> void putContentIfAbsent(final T ins) {
        context().putIfAbsent(ins.getClass().getName(), ins);
    }

    /** put参数 */
    public static <T> void putContentIfAbsent(final Object name, T ins) {
        context().putIfAbsent(name, ins);
    }

    /** 获取参数 */
    public static ThreadContext context() {
        ThreadContext threadContext = local.get();
        if (threadContext == null) {
            threadContext = new ThreadContext();
            local.set(threadContext);
        }
        return threadContext;
    }

    /** 设置参数 */
    public static void set() {
        local.set(new ThreadContext());
    }

    /** 设置参数 */
    public static void set(ThreadContext threadContext) {
        local.set(threadContext);
    }

    /** 清理缓存 */
    public static void cleanup() {
        local.remove();
    }

    /** 清理缓存 */
    public static void cleanup(Class<?> clazz) {
        context().remove(clazz.getName());
    }

    /** 清理缓存 */
    public static void cleanup(String name) {
        context().remove(name);
    }

    /** 清理缓存初始化的时候自动 clone 当前线程上下文 */
    @Getter
    public static abstract class ContextEvent implements Runnable {

        final ThreadContext threadContext;
        private final StackTraceElement stackTraceElement;

        protected ContextEvent(int stack) {
            threadContext = new ThreadContext(context());
            stackTraceElement = Thread.currentThread().getStackTrace()[stack];
        }

        public ContextEvent() {
            this(3);
        }

        @Override public void run() {
            try {
                local.set(threadContext);
                onEvent();
            } finally {
                cleanup();
            }
        }

        public abstract void onEvent();

        @Override public String toString() {
            return String.valueOf(stackTraceElement);
        }
    }

    /** 清理缓存初始化的时候自动 clone 当前线程上下文 */
    public static class ContextRunnable extends ContextEvent {

        private final Runnable task;

        public ContextRunnable(Runnable task) {
            super(3);
            this.task = task;
        }

        @Override public void onEvent() {
            task.run();
        }
    }

    public ThreadContext() {
    }

    public ThreadContext(Map m) {
        super(m);
    }

}
