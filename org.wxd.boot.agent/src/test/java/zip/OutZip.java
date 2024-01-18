package zip;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.wxd.boot.agent.zip.OutZipFile;
import org.wxd.boot.agent.zip.ReadZipFile;

import java.io.File;
import java.io.Serializable;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-05-07 19:08
 **/
@Slf4j
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
public class OutZip implements Serializable {

    @Test
    public void t() {
        try (OutZipFile outZip = new OutZipFile("d:\\test.zip")) {
            outZip.putZipEntry("a/a", "ddddddddd");
            outZip.putZipEntry("b/b", "ddddddddd");
        }
        try (ReadZipFile readZipFile = new ReadZipFile("d:\\test.zip")) {
            readZipFile.forEach(
                    (s, bytes) -> {
                        File file = new File(s);
                        System.out.println(file.getPath() + " - " + file.getName());
                    }
            );
        }
    }

}
