package org.wxd.boot.http.ssl;


import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.agent.io.FileReadUtil;
import org.wxd.boot.agent.io.FileUtil;
import org.wxd.boot.collection.concurrent.ConcurrentTable;
import org.wxd.boot.str.StringUtil;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.security.KeyStore;
import java.util.Collection;
import java.util.Map;

/**
 * https协议证书
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-12-18 16:18
 **/
@Slf4j
public class SslContextServer implements Serializable {

    private static final ConcurrentTable<SslProtocolType, String, SSLContext> sslContextMap = new ConcurrentTable<>();

    /***
     *
     * @param sslProtocolType ssl 类型
     * @param domain 域名
     * @return
     */
    public static SSLContext sslContext(SslProtocolType sslProtocolType, String domain) {
        Collection<File> jks = FileUtil.lists("jks");
        for (File jk : jks) {
            System.out.println(jk.getName());
        }
        return null;
    }

    public static SSLContext sslContext(SslProtocolType sslProtocolType, String jks_path, String jks_pwd_path) {

        if (sslProtocolType == null || StringUtil.emptyOrNull(jks_path) || StringUtil.emptyOrNull(jks_path))
            return null;

        Map<String, SSLContext> row = sslContextMap.row(sslProtocolType);
        return row.computeIfAbsent(jks_path, l -> {
            try (InputStream inputStream = FileUtil.findInputStream(jks_path)) {
                String jks_pwd = FileReadUtil.readString(jks_pwd_path);
                if (jks_pwd == null) {
                    jks_pwd = jks_pwd_path;
                }
                log.warn("jks文件：" + jks_path + ", 大小：" + inputStream.available());
                return initSSLContext(
                        jks_path,
                        sslProtocolType,
                        "jks",
                        inputStream,
                        jks_pwd,
                        jks_pwd);
            } catch (Exception e) {
                throw Throw.as("jks文件：" + jks_path, e);
            }
        });
    }

    public static SSLContext initSSLContext(String jks_path, SslProtocolType sslProtocolType,
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
