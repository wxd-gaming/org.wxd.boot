package org.wxd.boot.net.auth;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.wxd.boot.agent.io.FileUtil;
import org.wxd.boot.agent.io.FileWriteUtil;
import org.wxd.boot.agent.lang.Record2;
import org.wxd.boot.collection.OfSet;
import org.wxd.boot.lang.ObjectBase;
import org.wxd.boot.str.Md5Util;
import org.wxd.boot.str.xml.XmlUtil;
import org.wxd.boot.timer.MyClock;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-03-03 15:56
 **/
@Getter
@Setter
@Accessors(chain = true)
@Slf4j
@Root
public class SignConfig extends ObjectBase implements Serializable {

    private static SignConfig instance = null;

    public static SignConfig get() {
        if (instance == null) {
            try {
                instance = signConfig();
            } catch (Exception e) {
                log.error("读取配置数据", e);
            }
            if (instance == null) {
                try {
                    instance = new SignConfig();
                    instance.getUserList().add(
                            new SignUser()
                                    .setUserName("root")
                                    .setToken(Md5Util.md5DigestEncode(MyClock.millis() + ""))
                                    .setAuthority(OfSet.asSet(1, 2, 3, 4, 5, 6, 7, 8, 9))
                    );
                    String fileName = "config/sign-config.xml";
                    FileWriteUtil.writeString(fileName, instance.toXml());
                    System.out.println("初始化配置：" + fileName);
                } catch (Exception e) {
                    log.error("初始化配置", e);
                }
            }
        }
        return instance;
    }

    public static SignConfig signConfig() throws Exception {
        Record2<String, InputStream> inputStream = FileUtil.findInputStream("sign-config.xml");
        if (inputStream == null) {
            throw new RuntimeException("文件不存在 sign-config.xml");
        }
        System.out.println("读取配置：" + inputStream.t1());
        return signConfig(inputStream.t2());
    }

    public static SignConfig signConfig(InputStream stream) throws Exception {
        if (stream != null) {
            try {
                return XmlUtil.fromXml(stream, SignConfig.class);
            } finally {
                stream.close();
            }
        }
        return null;
    }

    /** 验签秘钥账户 */
    @ElementList(required = false, inline = true, entry = "sign")
    private List<IAuth> userList = new ArrayList<>();

    public <R extends IAuth> Optional<R> opt(String userName, String token) {
        return userList.stream()
                .filter(v -> Objects.equals(v.getUserName(), userName) && Objects.equals(v.getToken(), token))
                .findFirst()
                .map(v -> (R) v);
    }

    public <R extends IAuth> Optional<R> optByUser(String userName) {
        return userList.stream()
                .filter(v -> Objects.equals(v.getUserName(), userName))
                .findFirst()
                .map(v -> (R) v)
                ;
    }

    public <R extends IAuth> Optional<R> optToken(String token) {
        return userList.stream()
                .filter(v -> Objects.equals(v.getToken(), token))
                .findFirst()
                .map(v -> (R) v)
                ;
    }

}
