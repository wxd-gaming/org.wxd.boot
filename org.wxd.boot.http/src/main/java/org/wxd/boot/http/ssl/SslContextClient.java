package org.wxd.boot.http.ssl;


import org.wxd.boot.agent.exception.Throw;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.Serializable;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端调用的ssl验证
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-01-11 14:29
 **/
public class SslContextClient implements Serializable {

    private static final Map<SslProtocolType, SSLContext> sslContextMap = new ConcurrentHashMap<>();

    /**
     * @param sslName
     * @return
     */
    public static SSLContext sslContext(SslProtocolType sslName) {
        return sslContextMap.computeIfAbsent(sslName, l -> {
                    try {
                        SSLContext sslContext = SSLContext.getInstance(sslName.getTypeName());
                        // 实现一个X509TrustManager接口，用于绕过验证，不用修改里面的方法
                        sslContext.init(null,
                                new TrustManager[]{new TrustAnyTrustManager()},
                                new java.security.SecureRandom());
                        return sslContext;
                    } catch (Throwable throwable) {
                        throw Throw.as(throwable);
                    }
                }
        );
    }

    protected static class TrustAnyTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }

    }

}
