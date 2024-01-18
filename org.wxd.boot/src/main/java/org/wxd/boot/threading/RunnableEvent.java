package org.wxd.boot.threading;

import java.lang.reflect.Method;

/**
 * 任务类
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2024-01-12 19:15
 **/
public final class RunnableEvent extends Event {

    Runnable event;

    public RunnableEvent(Runnable event) {
        this.event = event;
    }

    public RunnableEvent(Method method, Runnable event) {
        super(method);
        this.event = event;
    }

    public RunnableEvent(long logTime, long warningTime, Runnable event) {
        super(logTime, warningTime);
        this.event = event;
    }

    public RunnableEvent(String taskInfoString, long logTime, long warningTime, Runnable event) {
        super(taskInfoString, logTime, warningTime);
        this.event = event;
    }

    @Override public void onEvent() throws Exception {
        event.run();
    }
}
