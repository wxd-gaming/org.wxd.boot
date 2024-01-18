package system;

import org.junit.Test;
import org.wxd.boot.agent.io.FileWriteUtil;

import java.io.File;
import java.nio.file.Files;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2024-01-12 20:04
 **/
public class FileTest {


    @Test
    public void t1() throws Exception {
        File file = new File("ok.txt");
        System.out.println(1);
        //try (FileOutputStream fileOutputStream = new FileOutputStream(file, false)) {
        //    fileOutputStream.write("sss".getBytes(StandardCharsets.UTF_8));
        //}
        FileWriteUtil.writeString(file, "ddd");
        System.out.println(2);
        file.deleteOnExit();
        System.out.println(3);
        System.gc();
        Thread.sleep(1000);
        System.out.println(4 + " - " + file.exists());
        file.delete();
        System.out.println(5 + " - " + file.exists());
    }

}
