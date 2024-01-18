package org.wxd.boot.timer;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.system.AnnUtil;
import org.wxd.boot.ann.Sort;
import org.wxd.boot.str.StringUtil;
import org.wxd.boot.system.GlobalUtil;
import org.wxd.boot.threading.Event;
import org.wxd.boot.timer.ann.Scheduled;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * cron 表达式时间触发器
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-09-27 10:40
 **/
@Slf4j
@Getter
@Setter
@Accessors(chain = true)
public class ScheduledInfo extends Event implements Comparable<ScheduledInfo> {

    private String name;
    private int index;
    private Object instance = null;
    private Method method = null;
    /** 和method是互斥的 */
    private Runnable scheduledTask;
    private TreeSet<Integer> secondSet = new TreeSet<>();
    private TreeSet<Integer> minuteSet = new TreeSet<>();
    private TreeSet<Integer> hourSet = new TreeSet<>();
    private TreeSet<Integer> dayOfWeekSet = new TreeSet<>();
    private TreeSet<Integer> dayOfMonthSet = new TreeSet<>();
    private TreeSet<Integer> monthSet = new TreeSet<>();
    private TreeSet<Integer> yearSet = new TreeSet<>();

    /** 上一次执行尚未完成是否持续执行 默认false 不执行 */
    private boolean scheduleAtFixedRate = false;
    protected AtomicBoolean runEnd = new AtomicBoolean(true);

    int cursecond = -1;
    protected long startExecTime;

    public ScheduledInfo(Object instance, Method method, Scheduled scheduled) {
        super(method);
        this.instance = instance;
        this.method = method;
        if (StringUtil.notEmptyOrNull(scheduled.name())) {
            this.name = "[scheduled-job]" + scheduled.name();
        } else {
            this.name = "[scheduled-job]" + instance.getClass().getName() + "." + method.getName();
        }

        final Sort sortAnn = AnnUtil.ann(method, Sort.class);
        this.index = sortAnn == null ? 999999 : sortAnn.value();
        this.scheduleAtFixedRate = scheduled.scheduleAtFixedRate();
        action(scheduled.value());
    }

    @Override public String getTaskInfoString() {
        return name;
    }

    /**
     * 秒 分 时 日 月 星期 年
     * <p> {@code * * * * * * * }
     * <p> 下面以 秒 配置举例
     * <p> * 或者 ? 无限制,
     * <p> 数字是 指定秒执行
     * <p> 0-5 第 0 秒 到 第 5 秒执行 每秒执行
     * <p> 0,5 第 0 秒 和 第 5 秒 各执行一次
     * <p> {@code *}/5 秒 % 5 == 0 执行
     * <p> 5/5 第五秒之后 每5秒执行一次
     * <p> 秒 0-59
     * <p> 分 0-59
     * <p> 时 0-23
     * <p> 日 1-28 or 29 or 30 or 31
     * <p> 月 1-12
     * <p> 星期 1-7 Mon Tues Wed Thur Fri Sat Sun
     * <p> 年 1970 - 2199
     */
    public ScheduledInfo(Runnable scheduledTask, String scheduledName, String scheduled, boolean scheduleAtFixedRate) {

        this.scheduledTask = scheduledTask;
        this.name = "[timer-job]" + scheduledTask.getClass() + "-" + scheduledName;

        this.index = 999999;
        this.scheduleAtFixedRate = scheduleAtFixedRate;
        action(scheduled);
    }

    /**
     * 用于获取下一次执行时间
     * <br>
     * <br>
     * 秒 分 时 日 月 星期 年
     * <p> {@code * * * * * * * }
     * <p> 下面以 秒 配置举例
     * <p> * 或者 ? 无限制,
     * <p> 数字是 指定秒执行
     * <p> 0-5 第 0 秒 到 第 5 秒执行 每秒执行
     * <p> 0,5 第 0 秒 和 第 5 秒 各执行一次
     * <p> {@code *}/5 秒 % 5 == 0 执行
     * <p> 5/5 第五秒之后 每5秒执行一次
     * <p> 秒 0-59
     * <p> 分 0-59
     * <p> 时 0-23
     * <p> 日 1-28 or 29 or 30 or 31
     * <p> 月 1-12
     * <p> 星期 1-7 Mon Tues Wed Thur Fri Sat Sun
     * <p> 年 1970 - 2199
     */
    public ScheduledInfo(String scheduled) {
        action(scheduled);
    }

    protected void action(String scheduled) {
        String[] values = new String[7];
        Arrays.fill(values, "*");

        if (StringUtil.notEmptyOrNull(scheduled)) {
            String[] split = scheduled.split(" ");
            for (int i = 0; i < split.length; i++) {
                if (StringUtil.emptyOrNull(split[i])) {
                    throw new RuntimeException("cron 表达式异常 [" + scheduled + "] 第 " + (i + 1) + " 个参数 空 不合法");
                }
                values[i] = split[i];
            }
        }

        action(secondSet, values[0], 0, 59);
        action(minuteSet, values[1], 0, 59);
        action(hourSet, values[2], 0, 23);
        action(dayOfMonthSet, values[3], 1, 31);
        action(monthSet, values[4], 1, 12);
        action(dayOfWeekSet, values[5], 1, 7);
        action(yearSet, values[6], 1970, 2199);
    }

    protected void action(TreeSet<Integer> set, String actionStr, int min, int max) {

        if ("*".equals(actionStr) || "?".equals(actionStr)) {
        } else if (actionStr.contains("-")) {
            String[] split = actionStr.split("-");
            int start = Integer.parseInt(split[0]);
            int end = Integer.parseInt(split[1]);
            if (start < min) {
                throw new RuntimeException(actionStr + " 起始值 小于最小值：" + min);
            }
            if (max < start) {
                throw new RuntimeException(actionStr + " 起始值 超过最大值：" + max);
            }
            if (end < min) {
                throw new RuntimeException(actionStr + " 结束值 小于最小值：" + min);
            }
            if (max < end) {
                throw new RuntimeException(actionStr + " 结束值 超过最大值：" + max);
            }
            if (start > end) {
                throw new RuntimeException(actionStr + " 起始值 大于 结束值" + max);
            }
            for (int i = start; i < end; i++) {
                set.add(i);
            }
        } else if (actionStr.contains("/")) {
            String[] split = actionStr.split("/");
            if (!"*".equals(split[0]) && !"?".equals(split[0])) {
                min = Integer.valueOf(split[0]);
            }
            int intv = Integer.parseInt(split[1]);

            for (int i = min; i <= max; i++) {
                if (i % intv == 0) {
                    set.add(i);
                }
            }

        } else if (actionStr.contains(",") || actionStr.contains("，")) {
            String[] split = actionStr.split(",|，");
            for (String s : split) {
                final Integer of = Integer.valueOf(s);
                if (min > of) {
                    throw new RuntimeException(actionStr + " 起始值 " + of + " 小于最小值：" + min);
                }
                if (of > max) {
                    throw new RuntimeException(actionStr + " 起始值 " + of + " 超过最大值：" + max);
                }
                set.add(of);
            }
        } else {
            set.add(Integer.valueOf(actionStr));
        }

    }

    public String nextDate() {
        return nextDate(MyClock.millis());
    }

    public String nextDate(long time) {
        return MyClock.formatDate(findValidateTime(time, 1000));
    }

    /** 获取开启时间 */
    public long nextTime() {
        return findValidateTime(MyClock.millis(), 1000);
    }

    public String upDate() {
        return upDate(MyClock.millis());
    }

    public String upDate(long time) {
        return MyClock.formatDate(findValidateTime(time, -1000));
    }

    /** 获取开启时间 */
    public long upTime() {
        return findValidateTime(MyClock.millis(), -1000);
    }

    /**
     * 获取开启时间
     *
     * @param time   时间磋
     * @param append 每一次变更的时间差查找上一次就是 -1000
     * @return
     */
    public long findValidateTime(long time, long append) {
        while (true) {
            int second = MyClock.getSecond(time);
            int minute = MyClock.getMinute(time);
            int hour = MyClock.getHour(time);
            int dayOfWeek = MyClock.dayOfWeek(time);
            int dayOfMonth = MyClock.dayOfMonth(time);
            int month = MyClock.getMonth(time);
            int year = MyClock.getYear(time);
            if (checkJob(second, minute, hour, dayOfWeek, dayOfMonth, month, year)) {
                return time;
            }
            time += append;
        }
    }

    public boolean checkJob(int second, int minute, int hour, int dayOfWeek, int dayOfMonth, int month, int year) {

        if (cursecond == second) {
            /*保证一秒内只执行一次*/
            return false;
        }

        cursecond = second;

        if (!secondSet.isEmpty()) {
            if (!secondSet.contains(second)) {
                return false;
            }
        }

        if (!minuteSet.isEmpty()) {
            if (!minuteSet.contains(minute)) {
                return false;
            }
        }

        if (!hourSet.isEmpty()) {
            if (!hourSet.contains(hour)) {
                return false;
            }
        }

        if (!dayOfWeekSet.isEmpty()) {
            if (!dayOfWeekSet.contains(dayOfWeek)) {
                return false;
            }
        }

        if (!dayOfMonthSet.isEmpty()) {
            if (!dayOfMonthSet.contains(dayOfMonth)) {
                return false;
            }
        }

        if (!monthSet.isEmpty()) {
            if (!monthSet.contains(month)) {
                return false;
            }
        }

        if (!yearSet.isEmpty()) {
            if (!yearSet.contains(year)) {
                return false;
            }
        }

        return true;
    }


    public void job(int second, int minute, int hour, int dayOfWeek, int dayOfMonth, int month, int year) {

        if (!checkJob(second, minute, hour, dayOfWeek, dayOfMonth, month, year)) {
            return;
        }
        if (!scheduleAtFixedRate && !runEnd.get()) return;
        /*标记为正在执行*/
        runEnd.set(false);

        if (this.isAsync()) {
            /*异步执行*/
            this.submit();
        } else {
            /*同步执行*/
            startExecTime = System.nanoTime();
            this.run();
            float v = (System.nanoTime() - startExecTime) / 10000 / 100f;
            if (v > logTime) {
                String msg = "执行：" + name + ", 耗时：" + v + " ms";
                log.info(msg);
            }
        }
    }

    @Override public void onEvent() {
        try {
            if (method != null) {
                method.invoke(instance);
            } else {
                scheduledTask.run();
            }
        } catch (Throwable throwable) {
            String msg = "执行：" + this.name;
            log.error(msg, throwable);
            GlobalUtil.exception(msg, throwable);
        } finally {
            /*标记为执行完成*/
            runEnd.set(true);
        }
    }

    @Override
    public int compareTo(ScheduledInfo o) {
        return Integer.compare(this.index, o.index);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScheduledInfo that = (ScheduledInfo) o;

        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return name;
    }

}
