package org.wxd.boot.httpclient.url;

import io.netty.util.AsciiString;
import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.agent.zip.GzipUtil;
import org.wxd.boot.httpclient.HttpHeadNameType;
import org.wxd.boot.httpclient.HttpHeadValueType;
import org.wxd.boot.httpclient.ssl.SslContextClient;
import org.wxd.boot.httpclient.ssl.SslProtocolType;
import org.wxd.boot.lang.SyncJson;
import org.wxd.boot.str.StringUtil;
import org.wxd.boot.system.GlobalUtil;
import org.wxd.boot.threading.Executors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 基于 HttpURLConnection 信息请求
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-11-15 12:17
 **/
@Slf4j
public abstract class HttpBase<H extends HttpBase> {

    protected final Response<H> response;
    protected HttpHeadValueType httpHeadValueType = HttpHeadValueType.Application;
    protected SslProtocolType sslProtocolType = SslProtocolType.SSL;
    protected final Map<String, String> reqHeaderMap = new LinkedHashMap<>();
    protected int connTimeout = 3000;
    protected int readTimeout = 3000;
    protected int retry = 1;
    /** 分段传输协议 */
    protected String boundary = null;
    protected String reqHttpMethod;

    protected HttpBase(String uriPath) {
        response = new Response(this, uriPath);
        header(HttpHeadNameType.Accept_Encoding, HttpHeadValueType.Gzip);
        header("user-agent", "java.org.wxd j21");
    }

    /** 处理需要发送的数据 */
    protected void writer(HttpURLConnection urlConnection) throws Exception {

    }

    public Response<H> request() {
        Throwable throwable = null;
        int r = 0;
        for (; r < retry; r++) {
            openURLConnection();
            try {
                writer(this.response.urlConnection);
                /*开始读取内容*/
                int responseCode = this.response.responseCode();
                InputStream inputStream;
                if (this.response.urlConnection.getErrorStream() != null) {
                    inputStream = this.response.urlConnection.getErrorStream();
                } else {
                    inputStream = this.response.urlConnection.getInputStream();
                }
                try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[512];
                    int len = -1;
                    while ((len = inputStream.read(buffer)) != -1) {
                        bos.write(buffer, 0, len);
                    }
                    byte[] toByteArray = bos.toByteArray();
                    String encoding = this.response.urlConnection.getContentEncoding();
                    if (encoding != null
                            && encoding.toLowerCase().contains("gzip")
                            && toByteArray.length > 0) {
                        this.response.bodys = GzipUtil.unGZip(toByteArray);
                    } else {
                        this.response.bodys = toByteArray;
                    }
                } finally {
                    inputStream.close();
                }
                return this.response;
            } catch (Throwable e) {
                throwable = e;
            } finally {
                if (this.response.urlConnection != null) {
                    this.response.urlConnection.disconnect();
                }
            }
        }
        throw Throw.as(this.response.toString() + ", 重试：" + r, throwable);
    }

    protected void openURLConnection() {
        try {
            URL realUrl = new URI(this.response.uriPath).toURL();

            this.response.urlConnection = (HttpURLConnection) realUrl.openConnection();

            if (this.response.urlConnection instanceof HttpsURLConnection httpsURLConnection) {
                httpsURLConnection.setSSLSocketFactory(SslContextClient.sslContext(sslProtocolType).getSocketFactory());
                httpsURLConnection.setHostnameVerifier(new TrustAnyHostnameVerifier());
            }

            this.response.urlConnection.setUseCaches(true);
            this.response.urlConnection.setConnectTimeout(connTimeout);
            this.response.urlConnection.setReadTimeout(readTimeout);

            if (httpHeadValueType == HttpHeadValueType.Multipart) {
                boundary = StringUtil.getRandomString(15);
                reqHeaderMap.put("content-type", httpHeadValueType.toString() + "; boundary=" + boundary);
            } else {
                reqHeaderMap.put("content-type", httpHeadValueType.toString());
            }

            for (Map.Entry<String, String> headerEntry : reqHeaderMap.entrySet()) {
                this.response.urlConnection.setRequestProperty(headerEntry.getKey(), headerEntry.getValue());
            }

            header("connection", "close");
            /*
            必须设置false，否则会自动redirect到重定向后的地址
            conn.setInstanceFollowRedirects(false);
             */

            /*get or post*/
            this.response.urlConnection.setRequestMethod(reqHttpMethod);
            if (log.isDebugEnabled()) {
                log.debug(reqHttpMethod + " " + sslProtocolType.getTypeName() + " " + this.response.uriPath);
                final String collect = this.response.urlConnection
                        .getRequestProperties()
                        .entrySet()
                        .stream()
                        .map(entry -> entry.getKey() + ":" + String.join("=", entry.getValue()))
                        .collect(Collectors.joining(", "));

                log.debug("http head：" + collect);
            }
            this.response.urlConnection.setDoInput(true);
            this.response.urlConnection.setDoOutput(true);

        } catch (Exception e) {
            throw Throw.as("请求的url：" + this.response.uriPath, e);
        }
    }

    protected CompletableFuture<Response<H>> completableFuture() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        response.threadStackTrace = new StackTraceElement[stackTrace.length - 1];
        System.arraycopy(stackTrace, 1, response.threadStackTrace, 0, response.threadStackTrace.length);
        return CompletableFuture.supplyAsync(() -> {
            this.request();
            return response;
        });
    }

    public void asyncBySyncJson(Consumer<SyncJson> ok) {
        async(httpBase -> ok.accept(SyncJson.parse(httpBase.bodyUnicodeDecodeString())));
    }

    public void asyncByString(Consumer<String> ok) {
        async(jPostText -> ok.accept(jPostText.bodyUnicodeDecodeString()));
    }

    public void asyncComplete(Consumer<Response<H>> complete) {
        async(null, complete);
    }

    public void async(Consumer<Response<H>> ok) {
        async(ok, null);
    }

    public void async(Consumer<Response<H>> ok, Consumer<Response<H>> complete) {
        async(ok, complete, this::actionThrowable);
    }

    /** 异步完成， */
    public CompletableFuture<Response<H>> async(Consumer<Response<H>> ok, Consumer<Response<H>> complete, Consumer<Throwable> error) {
        return completableFuture()
                .thenApply(r -> {
                    if (ok != null) {
                        try {
                            ok.accept(r);
                        } catch (Throwable throwable) {
                            if (error == null) {
                                this.actionThrowable(throwable);
                            } else {
                                error.accept(throwable);
                            }
                        }
                    }
                    return r;
                })
                .whenComplete((response, throwable) -> {
                    if (complete != null) {
                        try {
                            complete.accept(response);
                        } catch (Throwable t) {
                            if (error == null) {
                                this.actionThrowable(t);
                            } else {
                                error.accept(t);
                            }
                        }
                    }
                })
                .exceptionally(throwable -> {
                    if (throwable instanceof CompletionException) {
                        throwable = throwable.getCause();
                    }

                    if (error == null) {
                        log.error("异常 {}", response.uriPath, throwable);
                    } else {
                        RuntimeException aThrow = new Throw(throwable.toString());
                        aThrow.setStackTrace(response.threadStackTrace);
                        error.accept(aThrow);
                    }
                    return response;
                });
    }

    protected void actionThrowable(Throwable throwable) {
        log.error("{} url:{}", this.getClass().getSimpleName(), response.uriPath, throwable);
        if (retry > 1)
            GlobalUtil.exception(this.getClass().getSimpleName() + " url:" + response.uriPath, throwable);
    }

    public H timeout(int timeout) {
        this.connTimeout = timeout;
        this.readTimeout = timeout;
        return (H) this;
    }

    public H connTimeout(int timeout) {
        this.connTimeout = timeout;
        return (H) this;
    }

    public H readTimeout(int timeout) {
        this.readTimeout = timeout;
        return (H) this;
    }

    public H ssl(SslProtocolType sslProtocolType) {
        this.sslProtocolType = sslProtocolType;
        return (H) this;
    }

    /** 设置重试次数 */
    public H retry(int retry) {
        if (retry < 1) throw new RuntimeException("重试次数最少是1");
        this.retry = retry;
        return (H) this;
    }

    public H header(HttpHeadNameType headerKey, HttpHeadValueType HeaderValue) {
        header(headerKey.getValue(), HeaderValue.getValue());
        return (H) this;
    }

    public H header(HttpHeadNameType headerKey, String value) {
        header(headerKey.getValue(), value);
        return (H) this;
    }

    public H header(String name, HttpHeadValueType HeaderValue) {
        header(name, HeaderValue.getValue());
        return (H) this;
    }

    /**
     * 设置参数头
     *
     * @param headerKey   采用这个 HttpHeaderNames
     * @param HeaderValue
     * @return
     */
    public H header(AsciiString headerKey, String HeaderValue) {
        header(headerKey.toString(), HeaderValue);
        return (H) this;
    }

    public H header(String headerKey, String HeaderValue) {
        this.reqHeaderMap.put(headerKey, HeaderValue);
        return (H) this;
    }

    protected static class TrustAnyHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }

    }
}


