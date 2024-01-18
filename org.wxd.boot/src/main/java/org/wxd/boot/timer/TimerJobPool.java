package org.wxd.boot.timer;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.system.JvmUtil;
import org.wxd.boot.threading.Event;
import org.wxd.boot.threading.Executors;
import org.wxd.boot.threading.Job;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务调度器
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-09-24 18:21
 **/
@Slf4j
@Getter
public class TimerJobPool extends Event {

    protected Job job;

    /*                          类名字                  方法名    实例 */
    @Setter
    @Accessors(chain = true)
    protected List<ScheduledInfo> jobList = new ArrayList<>();

    public TimerJobPool() {
        super("任务调度器", 33, 500);
    }

    public void open() {
        job = Executors.getDefaultExecutor().scheduleAtFixedDelay("scheduled-timer", this, 10, 10, TimeUnit.MILLISECONDS);
        JvmUtil.addShutdownHook(this::close);
    }

    void close() {
        log.info("------------------------------关闭定时任务调度器------------------------------");
        if (job != null) {
            job.cancel();
            job = null;
        }
    }

    int curSecond = -1;

    @Override public void onEvent() {
        int second = MyClock.getSecond();
        if (curSecond == second) {
            return;
        }

        curSecond = second;

        int minute = MyClock.getMinute();
        int hour = MyClock.getHour();
        int dayOfWeek = MyClock.dayOfWeek();
        int dayOfMonth = MyClock.dayOfMonth();
        int month = MyClock.getMonth();
        int year = MyClock.getYear();

//                log.debug(second + "-" + minute + "-" + hour + "-" + dayOfWeek);

        for (ScheduledInfo scheduledInfo : jobList) {
            scheduledInfo.job(second, minute, hour, dayOfWeek, dayOfMonth, month, year);
        }
    }

}
