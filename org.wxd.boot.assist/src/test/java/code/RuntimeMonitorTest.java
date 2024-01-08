package code;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.wxd.boot.assist.AssistMonitor;
import org.wxd.boot.assist.IAssistMonitor;
import org.wxd.boot.assist.IAssistOutFile;
import org.wxd.boot.assist.MonitorAnn;

/**
 * assist 字节码测试
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2024-01-06 10:17
 **/
@Slf4j
public class RuntimeMonitorTest implements IAssistMonitor, IAssistOutFile {

    @Test
    @MonitorAnn(filter = true)
    public void at1() throws Exception {
        boolean start = AssistMonitor.start();
        /**如果要使用耗时统计添加启动参数 -javaagent:..\target\libs\assist.jar=需要监控的包名 */
        at2();
        at3();
        AssistMonitor.close(start, 3);
    }

    @Test
    @MonitorAnn(filter = true)
    public void at2() throws Exception {
        boolean start = AssistMonitor.start();
        /**如果要使用耗时统计添加启动参数 -javaagent:..\target\libs\assist.jar=需要监控的包名 */
        new B()
                .b1()
                .a1();
        AssistMonitor.close(start, 3);
    }

    public void at3() {
        System.out.println(1);
    }

    public static class A implements IAssistMonitor, IAssistOutFile {

        public A a1() throws Exception {
            a2();
            return this;
        }

        public void a2() throws InterruptedException {
            new T2().t2();
        }

    }

    public static class B extends A {

        public B b1() throws Exception {
            a1();
            return this;
        }

    }

}
