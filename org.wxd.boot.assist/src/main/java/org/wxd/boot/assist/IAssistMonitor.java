package org.wxd.boot.assist;

/**
 * 监控
 * -javaagent:..\target\libs\assist.jar=需要监控的报名
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2024-01-06 10:05
 **/
public interface IAssistMonitor {

    /** 时间记录 */
    @MonitorAlligator
    default long waringTime() {
        return 5;
    }

    @MonitorAlligator
    default void print(MonitorRecord monitorRecord) {
        AssistMonitor.printLog(monitorRecord.toString());
    }

}
