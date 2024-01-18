package code;


import com.carrotsearch.hppc.IntIntHashMap;
import org.junit.Test;
import org.wxd.boot.agent.io.FileReadUtil;
import org.wxd.boot.agent.io.FileUtil;
import org.wxd.boot.agent.io.FileWriteUtil;
import org.wxd.boot.agent.loader.JavaFileObject4StringCode;
import org.wxd.boot.append.JoinBuilder;
import org.wxd.boot.collection.ObjMap;
import org.wxd.boot.collection.OfSet;
import org.wxd.boot.collection.concurrent.ConcurrentHashSet;
import org.wxd.boot.format.TimeFormat;
import org.wxd.boot.lang.DayRecord;
import org.wxd.boot.str.json.FastJsonUtil;
import org.wxd.boot.system.JvmUtil;
import org.wxd.boot.system.ThrowableCache;
import org.wxd.boot.timer.MyClock;

import java.io.Serializable;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-03-03 16:08
 **/
public class Code implements Serializable {

    private static final long serialVersionUID = 1L;

    @Test
    public void joinBuilder() {
        final JoinBuilder joinBuilder = new JoinBuilder("[\n", ",", "\n]");
        joinBuilder
                .append(1).append(":").addLine(2)
                .append("d").append(":").addLine("ffff")
                .add("gggg");
        System.out.println(joinBuilder.toString());
    }

    @Test
    public void stringJoiner() {
        final StringJoiner stringJoiner = new StringJoiner(",");
        stringJoiner.add("1").add(":").add("2").add("d").add("\n").add("ffff").add("gggg");
        System.out.println(stringJoiner.toString());
    }

    @Test
    public void findName() {
        String s = FileReadUtil.readString("D:\\work\\loophero\\script\\src\\main\\java\\com\\popcorn\\loophero\\server\\map\\fightnew\\utils\\FightNewTargetUtil.java");
        System.out.println(JavaFileObject4StringCode.readFullClassName(s));
    }

    @Test
    public void t12() {
        long l = 1700220716591L;
        System.out.println(l);
        System.out.println(MyClock.formatDate(l));
        JvmUtil.setTimeZone("GMT+7");
        System.out.println(MyClock.formatDate(l));
    }

    @Test
    public void download() {
        FileUtil.downloadFile("https://www.cnblogs.com/ZhangHao-Study/p/16971276.html", "/out/sss.html");
    }

    @Test
    public void intMap() {
        IntIntHashMap intMap = new IntIntHashMap();
        System.out.println(intMap.put(1, 1));
        System.out.println(intMap.addTo(1, 1));
        String s = FastJsonUtil.toJson(intMap);
        System.out.println(s);
        IntIntHashMap parse = FastJsonUtil.parse(s, IntIntHashMap.class);
        Integer integer = parse.get(2);
        System.out.println(integer);
        System.out.println(parse);
        System.out.println(FastJsonUtil.toJson(parse));
    }

    @Test
    public void objMap() {
        ObjMap objMap = new ObjMap();
        objMap.put(1, 1);
        objMap.put("1", 2);
        String s = FastJsonUtil.toJson(objMap);
        System.out.println(s);
        ObjMap parse = FastJsonUtil.parse(s, ObjMap.class);
        System.out.println(parse.getInteger(2));
        System.out.println(parse.getIntValue(2));
        System.out.println(parse.getString("1"));
        System.out.println(parse);
    }

    @Test
    public void te() {
        String s = FastJsonUtil.toJson(TestEnum.First);
        System.out.println(s);
        TestEnum parse = FastJsonUtil.parse(s, TestEnum.class);
        System.out.println(parse);
    }

    @Test
    public void bytes() {
        HashMap<String, String> stringStringHashMap = new HashMap<>();
        stringStringHashMap.put("1", "2");
        System.out.println(FastJsonUtil.parse(FastJsonUtil.toBytes(stringStringHashMap)));
        System.out.println(FastJsonUtil.parse(FastJsonUtil.toJson(stringStringHashMap)));

        final TimeFormat timeFormat = new TimeFormat().addTime(65l * 1000 * 100);
        System.out.println(timeFormat.toString());
        System.out.println(timeFormat.toString(TimeFormat.FormatInfo.Second));
        System.out.println(timeFormat.toString(TimeFormat.FormatInfo.Minute));
        System.out.println(timeFormat.toString(TimeFormat.FormatInfo.Hour));
        System.out.println(timeFormat.toString(TimeFormat.FormatInfo.Day));
    }

    @Test
    public void clock1() {
        LocalDateTime localDateTime = MyClock.localDateTime();
        System.out.println(MyClock.dayOfMonth());
        System.out.println(MyClock.dayOfMonth(-1));
        System.out.println(20 + ((1000000d - MyClock.days()) / 10000000000d));
        System.out.println(MyClock.dayOfStartMillis() + " " + MyClock.dayOfEndMillis());
        System.out.println("Month - " + MyClock.getMonth() + " - " + MyClock.formatDate(MyClock.monthFirstDay()) + " - " + MyClock.formatDate(MyClock.monthLastDay()));
        System.out.println(MyClock.millis() + " , " + MyClock.localDateTime2Milli(2019, 4, 10, 15, 54, 0));
        System.out.println(MyClock.countDays(MyClock.addDayOfTime(-3)));
        System.out.println(MyClock.getYear() + " 年有 " + MyClock.yearDays() + " 天, " + MyClock.getMonth() + " 月有 " + MyClock.monthDays() + " 天");
        System.out.println("今天星期：" + MyClock.dayOfWeek() + " - " + MyClock.weekFirstDay(localDateTime) + " - " + MyClock.weekLastDay(localDateTime));
        System.out.println("今天 " + MyClock.nowString() + " 是几年的第：" + MyClock.dayOfYear() + " 天");

        System.out.println(MyClock.localDateTime().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss:SSS")));
    }

    @Test
    public void set() {
        ConcurrentHashSet<Integer> set = new ConcurrentHashSet<>();
        set.add(2);
        set.add(1);
        System.out.println(FastJsonUtil.toJson(set));
        System.out.println(FastJsonUtil.parse("[1,2]", ConcurrentHashSet.class));
    }

    @Test
    public void exception() {
        for (int i = 0; i < 5; i++) {
            try {
                e();
            } catch (Exception e) {
                System.out.println(ThrowableCache.addException(e));
            }
            try {
                e2();
            } catch (Exception e) {
                System.out.println(ThrowableCache.addException(e));
            }
            System.out.println("-----------------2-----------------");
        }
    }

    public void e() throws Exception {
        throw new Exception("a");
    }

    public void e2() throws Exception {
        throw new Exception("a");
    }

}
