package demo;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.wxd.boot.collection.ObjMap;
import org.wxd.boot.httpclient.jdk.HttpBuilder;

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

    @Test
    public void t1() throws Exception {
        String url;
        // url = "http://47.108.150.14:18800/sjcq/wanIp";
        url = "http://192.168.50.73:19000/test/ok";
        // url = "http://47.108.150.14:18801/test/ok";
        //url = "http://test-center.xiaw.net:18800/sjcq/wanIp";
        // url = "http://center.xiaw.net:18800/sjcq/wanIp";
        // url = "https://www.baidu.com";
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
        long l = System.nanoTime();
        for (int i = 0; i < testCount; i++) {
            long n = System.nanoTime();
            HttpBuilder.postMulti(url).putParams(ObjMap.build(1, 1)).readTimeout(200).retry(1).asyncString()
                    .subscribe(s -> {
                        allTime.addAndGet(System.nanoTime() - n);
                        source.incrementAndGet();
                    }).whenComplete((var, throwable) -> {
                        log.debug("{}", var, throwable);
                        atomicInteger.decrementAndGet();
                    });
        }
        while (atomicInteger.get() > 0) {}

        float v1 = (System.nanoTime() - l) / 10000 / 100f;
        float v = allTime.get() / 10000 / 100f;
        System.out.println(
                "jdk http client - " +
                        "请求 " + source.get() + " 次, " +
                        "耗时：" + v1 + "(累计耗时：" + v + ") ms, " +
                        "平均：" + v / source.get() + " ms, " +
                        "吞吐：" + ((testCount / v1 * 1000)) + "/s"
        );
        Thread.sleep(500);
    }
}
