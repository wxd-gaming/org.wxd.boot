package code;

import org.wxd.boot.assist.IAssistMonitor;
import org.wxd.boot.assist.IAssistOutFile;

public class T2 implements IAssistMonitor, IAssistOutFile {
    public void t2() throws InterruptedException {
        System.out.println(Thread.currentThread());
    }
}
