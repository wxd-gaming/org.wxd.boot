package org.wxd.boot.timer;

import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.agent.io.FileUtil;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 修改到最新api
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-07-29 10:33
 */
public class MyClock {

    private static final ConcurrentHashMap<String, ThreadLocal<SimpleDateFormat>> Time_Simple_Format = new ConcurrentHashMap<>();

    /** 获取一个线程安全的格式化对象， */
    public static SimpleDateFormat simpleDateFormat(String formatter) {
        return Time_Simple_Format.computeIfAbsent(
                        formatter,
                        k -> ThreadLocal.withInitial(() -> new SimpleDateFormat(k))
                )
                .get();
    }

    public static final ConcurrentSkipListMap<String, ThreadLocal<DateTimeFormatter>> SKIP_LIST_MAP = new ConcurrentSkipListMap<>();

    public static DateTimeFormatter dateTimeFormatter(String pa) {
        return SKIP_LIST_MAP.computeIfAbsent(pa, l -> ThreadLocal.withInitial(() -> DateTimeFormatter.ofPattern(l))).get();
    }


    /** 服务器时间偏移量 */
    public static final AtomicLong TimeOffset = new AtomicLong();

    /** yyyy-MM-dd */
    public static final String SDF_YYYYMMDD = "yyyy-MM-dd";
    /** yyyy_MM_dd */
    public static final String SDF_YYYYMMDD1 = "yyyy_MM_dd";
    /** yyyy-MM-dd HH */
    public static final String SDF_YYYYMMDDHH_1 = "yyyy-MM-dd HH";
    /** yyyy-MM-dd-HH */
    public static final String SDF_YYYYMMDDHH_2 = "yyyy-MM-dd-HH";
    /** yyyy_MM_dd_HH */
    public static final String SDF_YYYYMMDDHH_3 = "yyyy_MM_dd_HH";
    /** yyyy-MM-dd HH:mm */
    public static final String SDF_YYYYMMDDHHMM_1 = "yyyy-MM-dd HH:mm";
    /** yyyy-MM-dd-HH:mm */
    public static final String SDF_YYYYMMDDHHMM_2 = "yyyy-MM-dd-HH:mm";
    /** yyyy_MM_dd_HH_mm */
    public static final String SDF_YYYYMMDDHHMM_3 = "yyyy_MM_dd_HH_mm";
    /** yyyyMMddHHmm */
    public static final String SDF_YYYYMMDDHHMM_4 = "yyyyMMddHHmm";
    /** yyyy_MM/dd/HH_mm */
    public static final String SDF_YYYYMMDDHHMM_FILEPATH = "yyyy_MM/dd/HH_mm";
    /** yyyy-MM-dd-HH-mm */
    public static final String SDF_YYYYMMDDHHMM_5 = "yyyy-MM-dd-HH-mm";
    /** yyyy/MM/dd HH:mm:ss:SSS */
    public static final String SDF_YYYYMMDDHHMMSSSSS_1 = "yyyy/MM/dd HH:mm:ss:SSS";
    /** yyyy/MM/dd HH:mm:ss */
    public static final String SDF_YYYYMMDDHHMMSS_1 = "yyyy/MM/dd HH:mm:ss";
    /** yyyy-MM-dd HH:mm:ss */
    public static final String SDF_YYYYMMDDHHMMSS_2 = "yyyy-MM-dd HH:mm:ss";
    /** yyyy-MM-dd-HH-mm-ss */
    public static final String SDF_YYYYMMDDHHMMSS_3 = "yyyy-MM-dd-HH-mm-ss";
    /** yyyy_MM_dd_HH_mm_ss */
    public static final String SDF_YYYYMMDDHHMMSS_4 = "yyyy_MM_dd_HH_mm_ss";

    /**
     * utc 时间毫秒数
     *
     * @return 获取当前纪元 毫秒 1970-01-01T00:00:00Z.
     */
    public static long millis() {
        return System.currentTimeMillis() + TimeOffset.get();
    }

    public static Date date() {
        return new Date(millis());
    }

    /**
     * utc 时间秒数
     *
     * @return 获取当前纪元 秒 1970-01-01T00:00:00Z.
     */
    public static int second() {
        return (int) (millis() / 1000);
    }

    /**
     * utc 时间的分钟
     *
     * @return 获取当前纪元 分钟 1970-01-01T00:00:00Z.
     */
    public static int minute() {
        return second() / 60;
    }

    /** 当前时间毫秒时间磋 */
    public static long time2Milli(LocalDateTime time) {
        if (time == null) {
            return 0;
        }
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static LocalDate localDate() {
        return localDate(millis());
    }

    public static LocalDate localDate(long millis) {
        Instant instant = Instant.ofEpochMilli(millis);
        return LocalDate.ofInstant(instant, ZoneId.systemDefault());
    }

    public static LocalDateTime localDateTime() {
        return localDateTime(millis());
    }

    public static LocalDateTime localDateTime(long millis) {
        Instant instant = Instant.ofEpochMilli(millis);
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public long dayOffset(int dayOffset) {
        return dayOffset(millis(), dayOffset);
    }

    public long dayOffset(long date, int dayOffset) {
        LocalDateTime localDateTime = dayOffset(localDateTime(date), dayOffset);
        return time2Milli(localDateTime);
    }

    public LocalDateTime dayOffset(LocalDateTime localDateTime, int dayOffset) {
        return localDateTime.plusDays(dayOffset);
    }

    /** 获取今日时间格式,yyyy-MM-dd */
    public static String newDayString() {
        return formatDate(SDF_YYYYMMDD, millis());
    }

    /** yyyy/MM/dd HH:mm:ss */
    public static String nowString() {
        return formatDate(SDF_YYYYMMDDHHMMSS_2, millis());
    }

    /** 获取日期的时间格式 yyyy/MM/dd HH:mm:ss */
    public static String formatDate(long date) {
        return formatDate(SDF_YYYYMMDDHHMMSS_2, date);
    }

    /** 获取日期的时间格式 */
    public static String formatDate(String formatter) {
        return formatDate(formatter, millis());
    }

    /** 获取日期的时间格式 */
    public static String formatDate(String formatter, long date) {
        return formatDate(formatter, localDateTime(date));
    }

    /** 获取日期的时间格式 */
    public static String formatDate(String formatter, Date date) {
        return simpleDateFormat(formatter).format(date);
    }

    /** 获取日期的时间格式 */
    public static String formatDate(String formatter, LocalDateTime localDateTime) {
        return localDateTime.format(dateTimeFormatter(formatter));
    }

    /** 把指定格式的字符串还原成date */
    public static Date parseDate(String formatter, String date) {
        try {
            return simpleDateFormat(formatter).parse(date);
        } catch (ParseException e) {
            throw Throw.as("配置异常, 格式：" + formatter + ", 输入：" + date, e);
        }
    }

    /** 获取昨天时间格式,yyyy-MM-dd */
    public static String upDayString() {
        return upDayString("yyyy-MM-dd");
    }

    /** 获取昨天时间格式 */
    public static String upDayString(String format) {
        LocalDateTime localDateTime = localDateTime().plusDays(-1);
        return localDateTime.format(dateTimeFormatter(format));
    }

    /** 得到当前本地系统时间 周期天数 1970 年开始 */
    public static int days() {
        return days(millis());
    }

    /** 得到当前本地系统时间 周期天数 1970 年开始 */
    public static int days(Date time) {
        return days(time.getTime());
    }

    /** 得到当前本地系统时间 周期天数 1970 年开始 */
    public static int days(long time) {
        return days(localDate(time));
    }

    /** 得到当前本地系统时间 周期天数 1970 年开始 */
    public static int days(LocalDateTime time) {
        return days(time.toLocalDate());
    }

    /** 得到当前本地系统时间 周期天数 1970 年开始 */
    public static int days(LocalDate time) {
        return (int) time.toEpochDay();
    }

    /** 传入的时间和当前时间对比时间差 */
    public static int countDays(long time) {
        return countDays(millis(), time);
    }

    /**
     * 传入的时间和当前时间对比时间差
     *
     * @param startTime
     * @param endTime
     * @return
     */
    public static int countDays(long startTime, long endTime) {
        return days(endTime) - days(startTime);
    }

    /**
     * 传入的时间和当前时间对比时间差
     *
     * @param startTime
     * @param endTime
     * @return
     */
    public static int countDays(LocalDateTime startTime, LocalDateTime endTime) {
        return days(endTime) - days(startTime);
    }


    /**
     * 传入的时间和当前时间对比时间差
     *
     * @param startTime
     * @param endTime
     * @return
     */
    public static int countDays(LocalDate startTime, LocalDate endTime) {
        return days(endTime) - days(startTime);
    }

    /** 获取系统当前的星期 1-7 */
    public static int dayOfWeek() {
        return dayOfWeek(millis());
    }

    /** 获取传入毫秒的星期 1-7 */
    public static int dayOfWeek(long time) {
        LocalDateTime localDateTime = localDateTime(time);
        return dayOfWeek(localDateTime);
    }

    /** 获取传入毫秒的星期 1-7 */
    public static int dayOfWeek(LocalDateTime localDateTime) {
        return localDateTime.getDayOfWeek().getValue();
    }

    /** 获取系统当前月份中的日 1 - 31 */
    public static int dayOfMonth() {
        return dayOfMonth(millis());
    }

    /** 获取传入毫秒的月份中的日 1 - 31 */
    public static int dayOfMonth(long time) {
        return dayOfMonth(time, 0);
    }

    /**
     * 获取时间当月的第几天 1 - 31
     *
     * @param dayCount 如果往后加一天 1 往前一天是 -1
     * @return
     */
    public static int dayOfMonth(int dayCount) {
        return dayOfMonth(millis(), dayCount);
    }

    /**
     * 获取时间当月的第几天 1 - 31
     *
     * @param time
     * @param dayCount 如果往后加一天 1 往前一天是 -1
     * @return
     */
    public static int dayOfMonth(long time, int dayCount) {
        return dayOfMonth(localDateTime(time), dayCount);
    }

    public static int dayOfMonth(LocalDateTime localDateTime, int dayCount) {
        return localDateTime.plusDays(dayCount).getDayOfMonth();
    }

    /** 获取当前日期的星期1 00:00:00 */
    public static long weekFirstDay() {
        return weekFirstDay(millis());
    }

    /** 获取当前日期的星期1 00:00:00 */
    public static long weekFirstDay(long time) {
        LocalDateTime localDateTime = localDateTime(time);
        return time2Milli(weekFirstDay(localDateTime));
    }

    /** 获取当前日期的星期1 00:00:00 */
    public static LocalDateTime weekFirstDay(LocalDateTime localDateTime) {
        LocalDateTime localDateTime1 = LocalDateTime.of(localDateTime.toLocalDate(), LocalTime.MIN);
        return localDateTime1.with(DayOfWeek.MONDAY);
    }

    /** 获取当前日期的星期天 23:59:59 */
    public static long weekLastDay() {
        return weekLastDay(millis());
    }

    /** 获取当前日期的星期天 23:59:59 */
    public static long weekLastDay(long time) {
        LocalDateTime localDateTime = localDateTime(time);
        return time2Milli(weekLastDay(localDateTime));
    }

    /** 获取当前日期的星期天 23:59:59 */
    public static LocalDateTime weekLastDay(LocalDateTime localDateTime) {
        LocalDateTime localDateTime1 = LocalDateTime.of(localDateTime.toLocalDate(), LocalTime.MAX);
        return localDateTime1.with(DayOfWeek.SUNDAY);
    }

    /** 获取时间当月的第一天00:00:00 */
    public static long monthFirstDay() {
        return monthFirstDay(millis());
    }

    /** 获取时间当月的第一天00:00:00 */
    public static long monthFirstDay(long time) {
        return time2Milli(monthFirstDay(localDateTime(time)));
    }

    /** 获取时间当月的第一天00:00:00 */
    public static LocalDateTime monthFirstDay(LocalDateTime localDateTime) {
        return LocalDateTime.of(localDateTime.toLocalDate(), LocalTime.MIN).with(TemporalAdjusters.firstDayOfMonth());
    }

    /** 获取时间当月的最后一天23:59:59 */
    public static long monthLastDay() {
        return monthLastDay(millis());
    }

    /** 获取时间当月的最后一天23:59:59 */
    public static long monthLastDay(long time) {
        return time2Milli(monthLastDay(localDateTime(time)));
    }

    /** 获取时间当月的最后一天23:59:59 */
    public static LocalDateTime monthLastDay(LocalDateTime localDateTime) {
        return LocalDateTime.of(localDateTime.toLocalDate(), LocalTime.MAX).with(TemporalAdjusters.lastDayOfMonth());
    }

    /** 获取当前时间月有多少天 */
    public static int monthDays() {
        return monthDays(localDateTime());

    }

    /** 获取当前时间月有多少天 */
    public static int monthDays(LocalDateTime localDateTime) {
        return localDateTime.toLocalDate().lengthOfMonth();
    }

    public static int yearDays() {
        return yearDays(localDateTime());
    }

    /** 获取当前时间年有多少天 */
    public static int yearDays(LocalDateTime localDateTime) {
        return localDateTime.toLocalDate().lengthOfYear();
    }

    /** 获取系统当前月份 1-12 */
    public static int getMonth() {
        return getMonth(millis());
    }

    /** 获取传入毫秒的月份 1-12 */
    public static int getMonth(long time) {
        return localDateTime(time).getMonth().getValue();
    }

    /** 获取系统当前小时 0 to 23 */
    public static int getHour() {
        return getHour(millis());
    }

    /** 获取传入毫秒的当日小时 0 to 23 */
    public static int getHour(long time) {
        return localDateTime(time).getHour();
    }

    /** 获取系统当前分钟 0-59 */
    public static int getMinute() {
        return getMinute(millis());
    }

    /** 获取传入毫秒的当日分钟 0-59 */
    public static int getMinute(long time) {
        return localDateTime(time).getMinute();
    }

    /** 获取系统当前秒 0-59 */
    public static int getSecond() {
        return getSecond(millis());
    }

    /** 获取传入毫秒的秒 0-59 */
    public static int getSecond(long time) {
        return localDateTime(time).getSecond();
    }

    /** 获取年 */
    public static int getYear() {
        return getYear(millis());
    }

    /** 获取年 */
    public static int getYear(long time) {
        LocalDateTime localDateTime = localDateTime(time);
        return getYear(localDateTime);
    }

    /** 获取年 */
    public static int getYear(LocalDateTime localDateTime) {
        return localDateTime.getYear();
    }

    /** 获取系统当前的天 1 to 365, or 366 */
    public static int dayOfYear() {
        return dayOfYear(millis());
    }

    /** 获取传入毫秒的天 1 to 365, or 366 */
    public static int dayOfYear(long time) {
        LocalDateTime localDateTime = localDateTime(time);
        return dayOfYear(localDateTime);
    }

    /** 获取传入毫秒的天 1 to 365, or 366 */
    public static int dayOfYear(LocalDateTime localDateTime) {
        return localDateTime.getDayOfYear();
    }

    /** 验证时间数值是不是今天 */
    public static boolean isSameDay(long time) {
        return isSameDay(millis(), time);
    }

    /** 判断两个时间是否在同一天 */
    public static boolean isSameDay(long time1, long time2) {
        return countDays(time1, time2) == 0;
    }

    public static boolean isSameWeek(long time) {
        return isSameWeek(millis(), time);
    }

    /**
     * 判断两个时间是否在同一周(注意这里周日和周一判断是在一周里的)
     *
     * @param time1
     * @param time2
     * @return
     */
    public static boolean isSameWeek(long time1, long time2) {
        return weekFirstDay(time1) == weekFirstDay(time2);
    }

    /**
     * 判断两个时间是否在同一月
     *
     * @param time1
     * @param time2
     * @return
     */
    public static boolean isSameMonth(long time1, long time2) {
        return monthFirstDay(time1) == monthFirstDay(time2);
    }

    /**
     * 判断两个时间是否在同一季度
     *
     * @param time1
     * @param time2
     * @return
     */
    public static boolean isSameQuarter(long time1, long time2) {
        int y1 = getYear(time1);
        int y2 = getYear(time2);
        int m1 = getMonth(time1);
        int m2 = getMonth(time2);
        return y1 == y2 && m1 / 4 == m2 / 4;
    }

    /**
     * 判断两个时间是否在同一年
     *
     * @param time1
     * @param time2
     * @return
     */
    public static boolean isSameYear(long time1, long time2) {
        return getYear(time1) == getYear(time2);
    }

    /**
     * 返回指定日期的季度第一天 yyyy-MM-dd
     *
     * @return
     */
    public static String getQuarterOfYear() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis());
        int month = cal.get(Calendar.MONTH);
        int newmonth = month % 3;
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.MONTH, month - newmonth);
        return formatDate(SDF_YYYYMMDD, cal.getTime());
    }

    /**
     * 获取每一个月的第一天的日期 ,yyyy-MM-dd
     *
     * @return
     */
    public static String getDateMonthDayString() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis());
        cal.set(Calendar.DAY_OF_MONTH, 1);
        return formatDate(SDF_YYYYMMDD, cal.getTime());
    }

    /**
     * 获取每一年的第一天的日期,yyyy-MM-dd
     *
     * @return
     */
    public static String getDateYearDayString() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis());
        cal.set(Calendar.DAY_OF_YEAR, 1);
        return formatDate(SDF_YYYYMMDD, cal.getTime());
    }

    public static long currentOffsetTimeMillis(int offsetDays, int hour, int minute, int second) {
        return millis()
                + TimeUnit.DAYS.toMillis(offsetDays)
                + TimeUnit.HOURS.toMillis(hour)
                + TimeUnit.MINUTES.toMillis(minute)
                + TimeUnit.SECONDS.toMillis(second);
    }

    /** 根据传入的年月日时分秒 返回一个毫秒数 */
    public static long localDateTime2Milli(int year, int month, int day, int hour, int minute, int second) {
        return time2Milli(localDateTime(year, month, day, hour, minute, second));
    }

    public static LocalDateTime localDateTime(int year, int month, int day, int hour, int minute, int second) {
        return LocalDateTime.of(year, month, day, hour, minute, second);
    }

    /** 当前时间加上指定小时， */
    public static long addMinutesOfTime(int minutes) {
        return millis() + TimeUnit.MINUTES.toMillis(minutes);
    }

    /** 当前时间加上指定小时， */
    public static long addHourOfTime(int hour) {
        return millis() + TimeUnit.HOURS.toMillis(hour);
    }

    /** 当前时间加上指定天数后的日期， */
    public static long addDayOfTime(int days) {
        return millis() + TimeUnit.DAYS.toMillis(days);
    }

    /**
     * 清理目录下文件
     *
     * @param fileDays 文件的有效期限，文件最后的修改日期和当前日期对比相差的天数
     * @param filePath 文件路径，会递归查找路径下的所有文件
     * @param names    文件的后缀名
     */
    public static void clearFile(int fileDays, File filePath, String... names) {
        try {
            FileUtil.walkFiles(filePath, names)
                    .sorted(Comparator.reverseOrder())
                    .forEach((checkFile) -> {
                        if (countDays(millis(), checkFile.lastModified()) > fileDays) {
                            checkFile.delete();
                            final File parentFile = checkFile.getParentFile();
                            if (parentFile != null && parentFile.isDirectory()) {
                                final File[] files = parentFile.listFiles();
                                if (files == null || files.length < 1) {
                                    /*删除空文件夹*/
                                    parentFile.delete();
                                }
                            }
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }


    /** 获取指定日期，开始时间，00:00:00 */
    public static long dayOfStartMillis() {
        return dayOfStartMillis(millis());
    }

    /**
     * 获取指定日期，开始时间，00:00:00
     *
     * @param time 指定起始时间偏移 单位是 秒 时间磋
     * @return
     */
    public static long dayOfStartMillis(int time) {
        return dayOfStartMillis(time, 0);
    }

    /**
     * 获取指定日期，开始时间，00:00:00
     *
     * @param time   指定起始时间偏移 单位是 秒 时间磋
     * @param addDay 指定偏移的天数
     * @return
     */
    public static long dayOfStartMillis(int time, int addDay) {
        return dayOfStartMillis(time * 1000L, addDay);
    }

    /**
     * 获取指定日期，开始时间，00:00:00
     *
     * @param time 指定起始时间偏移 单位是 毫秒 时间磋
     * @return
     */
    public static long dayOfStartMillis(long time) {
        return dayOfStartMillis(time, 0);
    }


    /**
     * 获取指定日期，开始时间，00:00:00
     *
     * @param time   指定起始时间偏移 单位是 毫秒 时间磋
     * @param addDay 指定偏移的天数
     * @return
     */
    public static long dayOfStartMillis(long time, int addDay) {
        return dayOfStartMillis(localDateTime(time), addDay);
    }

    /**
     * 获取指定日期，开始时间，00:00:00
     *
     * @param time   指定起始时间偏移 单位是 毫秒 时间磋
     * @param addDay 指定偏移的天数
     * @return
     */
    public static long dayOfStartMillis(LocalDateTime time, int addDay) {
        return time2Milli(dayOfStart(time, addDay));
    }

    /**
     * 获取指定日期，开始时间，00:00:00
     *
     * @param time   指定起始时间偏移 单位是 毫秒 时间磋
     * @param addDay 指定偏移的天数
     * @return
     */
    public static LocalDateTime dayOfStart(LocalDateTime time, int addDay) {
        return time.plusDays(addDay).with(LocalTime.MIN);
    }

    /**
     * 获取指定日期，结束时间，23:59:59
     *
     * @return
     */
    public static long dayOfEndMillis() {
        return dayOfEndMillis(0);
    }

    /**
     * 获取指定日期，结束时间，23:59:59
     *
     * @param addDay 指定偏移的天数
     * @return
     */
    public static long dayOfEndMillis(int addDay) {
        return dayOfEndMillis(millis(), addDay);
    }

    /**
     * 获取指定日期，结束时间，23:59:59
     *
     * @param time   指定起始时间偏移 单位是 秒 时间磋
     * @param addDay 指定偏移的天数
     * @return
     */
    public static long dayOfEndMillis(int time, int addDay) {
        return dayOfEndMillis(time * 1000L, addDay);
    }

    /**
     * 获取指定时间的当天 结束时间，23:59:59
     *
     * @param time 指定起始时间偏移 单位是 毫秒 时间磋
     * @return
     */
    public static long dayOfEndMillis(long time) {
        return dayOfEndMillis(time, 0);
    }

    /**
     * 获取指定日期，结束时间，23:59:59
     *
     * @param time   指定起始时间偏移 单位是 毫秒 时间磋
     * @param addDay 指定偏移的天数
     * @return
     */
    public static long dayOfEndMillis(long time, int addDay) {
        return dayOfEndMillis(localDateTime(time), addDay);
    }

    public static long dayOfEndMillis(LocalDateTime time, int addDay) {
        return time2Milli(dayOfEnd(time, addDay));
    }

    public static LocalDateTime dayOfEnd(LocalDateTime time, int addDay) {
        return time.plusDays(addDay).with(LocalTime.MAX);
    }


    /** 用秒表示今天日期 */
    public static int dayOfSecond() {
        return dayOfSecond(millis());
    }

    /** 用秒表示今天日期 */
    public static int dayOfSecond(long millis) {
        LocalDateTime localDateTime = localDateTime(millis);
        return (int) (TimeUnit.HOURS.toSeconds(localDateTime.getHour())
                + TimeUnit.HOURS.toSeconds(localDateTime.getMinute())
                + localDateTime.getSecond());
    }

    /** 获取今天时间用分表示 */
    public static int dayOfMinute() {
        return dayOfMinute(millis());
    }

    /** 今天是时间用分表示 */
    public static int dayOfMinute(long millis) {
        LocalDateTime localDateTime = localDateTime(millis);
        return (int) (TimeUnit.HOURS.toMinutes(localDateTime.getHour()) + localDateTime.getMinute());
    }

}
