package code;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.wxd.boot.collection.ObjMap;
import org.wxd.boot.httpclient.url.HttpBuilder;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 测试
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-12-28 11:17
 **/
@Slf4j
public class HttpTest {

    public static void main(String[] args) throws Exception {
        new HttpTest().t1();
    }

    @Test
    public void t1() throws Exception {
        String url;
        // url = "http://47.108.150.14:18800/sjcq/wanIp";
        //url = "http://192.168.50.73:19000/test/ok";
        // url = "http://47.108.150.14:18801/test/ok";
        //url = "http://center.xiaw.net:18800/sjcq/wanIp";
        // url = "http://center.xiaw.net:18800/sjcq/wanIp";
        url = "http://47.108.81.97:18001/index";
        //url = "https://www.baidu.com";
        tv1(url, 1);
        tv1(url, 10);
        tv1(url, 50);
        tv1(url, 100);
        tv1(url, 500);
        tv1(url, 1000);
    }

    public void tv1(String url, int testCount) throws Exception {
        AtomicInteger atomicInteger = new AtomicInteger(testCount);
        AtomicInteger source = new AtomicInteger();
        AtomicLong allTime = new AtomicLong();
        AtomicLong exCount = new AtomicLong();
        long l = System.nanoTime();
        for (int i = 0; i < testCount; i++) {
            long n = System.nanoTime();
            AtomicInteger tmp = new AtomicInteger();
            HttpBuilder.postMulti(url).putParams(ObjMap.build(1, 1)).timeout(1200).readTimeout(8000).logTime(5000).waringTime(5000).retry(2).asyncString()
                    .subscribe(s -> {
                        allTime.addAndGet(System.nanoTime() - n);
                        source.incrementAndGet();
                        tmp.incrementAndGet();
                    }).whenComplete((var, throwable) -> {
                        if (tmp.get() == 0) {
                            int p = 0;
                        }
                        if (throwable != null) exCount.incrementAndGet();
                        log.debug("{}", var, throwable);
                        atomicInteger.decrementAndGet();
                    });
        }
        while (atomicInteger.get() > 0) {}

        float v1 = (System.nanoTime() - l) / 10000 / 100f;
        float v = allTime.get() / 10000 / 100f;
        System.out.printf(
                "HttpURLConnection - 请求 %4d 次, 完成 %4d 次, 异常：%4d 次, 耗时：%8.2f(累计耗时：%12.2f) ms, 平均：%8.2f ms, 吞吐：%8.2f/s%n",
                testCount,
                source.get(),
                exCount.get(),
                v1, v, v / source.get(), ((testCount / v1 * 1000))
        );
        System.gc();
        Thread.sleep(1500);
    }
}
