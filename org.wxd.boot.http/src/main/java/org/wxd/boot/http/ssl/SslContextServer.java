package org.wxd.boot.http.ssl;


import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.io.FileReadUtil;
import org.wxd.boot.agent.io.FileUtil;
import org.wxd.boot.agent.lang.Record2;
import org.wxd.boot.collection.concurrent.ConcurrentTable;
import org.wxd.boot.str.StringUtil;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.io.Serializable;
import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * https协议证书
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-12-18 16:18
 **/
@Slf4j
public class SslContextServer implements Serializable {

    private static final ConcurrentTable<SslProtocolType, String, SSLContext> sslContextMap = new ConcurrentTable<>();

    public static SSLContext sslContext(SslProtocolType sslProtocolType, String jks_path, String jks_pwd_path) {

        if (sslProtocolType == null || StringUtil.emptyOrNull(jks_path) || StringUtil.emptyOrNull(jks_path))
            return null;

        Map<String, SSLContext> row = sslContextMap.row(sslProtocolType);
        return row.computeIfAbsent(jks_path, l -> {
            try {
                AtomicReference<InputStream> streams = new AtomicReference<>();
                AtomicReference<String> pwd = new AtomicReference<>();

                // 获取当前运行的JAR文件
                Record2<String, InputStream> jksStream = FileUtil.findInputStream(SslContextServer.class.getClassLoader(), jks_path);
                streams.set(jksStream.t2());
                System.out.printf("读取文件目录：jks=%s, 文件大小：%s\n", jksStream.t1(), jksStream.t2().available());
                pwd.set(jks_pwd_path);

                Record2<String, InputStream> pwdStream = FileUtil.findInputStream(SslContextServer.class.getClassLoader(), jks_pwd_path);
                if (pwdStream != null) {
                    // 判断是否为资源文件
                    System.out.printf("读取文件目录：jkspwd=%s\n", pwdStream.t1());
                    pwd.set(FileReadUtil.readString(pwdStream.t2()));
                }
                return initSSLContext(sslProtocolType, "jks", streams.get(), pwd.get(), pwd.get());
            } catch (Exception e) {
                throw new RuntimeException("读取jks文件异常", e);
            }
        });
    }

    public static SSLContext initSSLContext(SslProtocolType sslProtocolType,
                                            String keyStoreType,
                                            InputStream keyStoreStream,
                                            String certificatePassword,
                                            String keystorePassword) throws Exception {
        // 密钥管理器
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        // 密钥库KeyStore
        KeyStore ks = KeyStore.getInstance(keyStoreType);
        // 加载服务端的KeyStore  ；sNetty是生成仓库时设置的密码，用于检查密钥库完整性的密码
        ks.load(keyStoreStream, certificatePassword.toCharArray());
        // 初始化密钥管理器
        kmf.init(ks, keystorePassword.toCharArray());
        tmf.init(ks);
        // 获取安全套接字协议（TLS协议）的对象
        SSLContext sslContext = SSLContext.getInstance(sslProtocolType.getTypeName());
        // 初始化此上下文
        // 参数一：认证的密钥      参数二：对等信任认证  参数三：伪随机数生成器 。 由于单向认证，服务端不用验证客户端，所以第二个参数为null
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return sslContext;
    }

}
