package org.wxd.boot.threading;

import lombok.Getter;
import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.agent.system.AnnUtil;
import org.wxd.boot.assist.IAssistMonitor;
import org.wxd.boot.str.StringUtil;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 获取任务定义的注释名称
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-11-09 10:21
 **/
@Getter
public abstract class Event implements Runnable, IAssistMonitor {

    protected String taskInfoString = "";
    /** 输出日志的时间 */
    protected long logTime = 33;
    /** 执行告警时间 */
    protected long warningTime = 1000;
    /** 是否使用虚拟线程，如果支持指定了 threadName 这个值无效 */
    protected boolean vt = false;
    /** 队列名称 */
    protected String threadName = "";
    /** 队列名称 */
    protected String queueName = "";
    protected boolean async = false;

    public Event() {
    }

    public Event(Method method) {
        /* 虚拟线程 */
        AtomicBoolean vt = new AtomicBoolean();
        AtomicReference<String> threadName = new AtomicReference<>();
        AtomicReference<String> queueName = new AtomicReference<>();

        this.async = AsyncImpl.asyncAction(vt, threadName, queueName, method);
        this.vt = vt.get();
        this.threadName = threadName.get();
        this.queueName = queueName.get();

        AnnUtil.annOpt(method, ExecutorLog.class).ifPresent(executorLog -> {
            logTime = executorLog.logTime();
            warningTime = executorLog.warningTime();
        });
    }

    public Event(long logTime, long warningTime) {
        this.logTime = logTime;
        this.warningTime = warningTime;
    }

    public Event(String taskInfoString, long logTime, long warningTime) {
        this.taskInfoString = taskInfoString;
        this.logTime = logTime;
        this.warningTime = warningTime;
    }

    @Override public final void run() {
        try {
            onEvent();
        } catch (Exception e) {
            throw Throw.as(e);
        }
    }

    public abstract void onEvent() throws Exception;

    /** 提交待线程池执行 */
    public final void submit() {
        IExecutorServices executor;
        if (StringUtil.notEmptyOrNull(getThreadName())) {
            executor = Executors.All_THREAD_LOCAL.get(getThreadName());
        } else if (isVt()) {
            executor = Executors.getLogicExecutor();
        } else {
            executor = Executors.getLogicExecutor();
        }
        executor.submit(getQueueName(), this, 4);
    }

}
