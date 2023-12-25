package org.wxd.boot.httpclient.jdk;

import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.threading.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.cert.X509Certificate;
import java.time.Duration;

/**
 * 基于 java 原生的http协议支持
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-10-13 12:22
 **/
@Slf4j
public class HttpBuilder {

    protected static final HttpClient HTTP_CLIENT;

    static {
        try {
            SSLContext tls = SSLContext.getInstance("tls");
            X509TrustManager tm = new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {return null;}

                public void checkClientTrusted(X509Certificate[] xcs, String str) {}

                public void checkServerTrusted(X509Certificate[] xcs, String str) {}
            };
            tls.init(null, new TrustManager[]{tm}, null);
            HTTP_CLIENT = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofMillis(3000))
                    .sslContext(tls)
                    .executor(Executors.getLogicExecutor())
//                .proxy(ProxySelector.of(new InetSocketAddress("proxy.example.com", 80)))
//                .authenticator(Authenticator.getDefault())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Get get(String url) {
        return new Get(HTTP_CLIENT, url);
    }

    public static Get get(HttpClient httpClient, String url) {
        return new Get(httpClient, url);
    }

    /** 构建好的字符串 */
    public static PostText postText(String url) {
        return postText(HTTP_CLIENT, url);
    }

    /** 构建好的字符串 */
    public static PostText postText(HttpClient httpClient, String url) {
        return new PostText(httpClient, url);
    }

    /** 多段参数设置 */
    public static PostMulti postMulti(String url) {
        return new PostMulti(HTTP_CLIENT, url);
    }

    /** 多段参数设置 */
    public static PostMulti postMulti(HttpClient httpClient, String url) {
        return new PostMulti(httpClient, url);
    }

}
