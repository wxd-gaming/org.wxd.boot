package threading;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.wxd.boot.threading.Executors;

/**
 * 线程测试
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-05-29 21:31
 **/
@Slf4j
public class Thread2 {

    @Test
    public void t12() throws InterruptedException {
        // execute("1", () -> {
        //     execute("1", () -> {
        //         log.debug("{} {}", this.hashCode(), 1);
        //     });
        // });

        execute("", new Runnable() {
            @Override public void run() {
                execute("1", new Runnable() {
                    @Override public void run() {
                        int code = this.hashCode();
                        log.debug("{} {} {}", Executors.currentThreadQueueKey(), code, "0-1");
                    }
                });
            }
        });

        execute("1", new Runnable() {
            @Override public void run() {
                execute("2", new Runnable() {
                    @Override public void run() {
                        int code = this.hashCode();
                        log.debug("{} {} {}", Executors.currentThreadQueueKey(), code, "1-2");
                    }
                });
            }
        });
        execute("1", new Runnable() {
            @Override public void run() {
                execute("1", new Runnable() {
                    @Override public void run() {
                        int code = this.hashCode();
                        log.debug("{} {} {}", Executors.currentThreadQueueKey(), code, "1-1");
                    }
                });
            }
        });
        execute("2", new Runnable() {
            @Override public void run() {
                execute("1", new Runnable() {
                    @Override public void run() {
                        int code = this.hashCode();
                        log.debug("{} {} {}", Executors.currentThreadQueueKey(), code, "2-1");
                    }
                });
            }
        });

        Thread.sleep(1500);
    }

    public void execute(String qk, Runnable runnable) {
        if (Executors.checkCurrentThread(qk)) {
            log.debug("{} {} {}", qk, runnable.hashCode(), true);
            runnable.run();
            return;
        }
        log.debug("{} {} {}", qk, runnable.hashCode(), false);
        Executors.getLogicExecutor().submit(qk, runnable);
    }

    @Override public int hashCode() {
        return super.hashCode();
    }
}
