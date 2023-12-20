package org.wxd.boot.httpclient.jdk;

import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.httpclient.HttpHeadNameType;
import org.wxd.boot.httpclient.HttpHeadValueType;
import org.wxd.boot.lang.SyncJson;
import org.wxd.boot.str.StringUtil;
import org.wxd.boot.system.GlobalUtil;
import org.wxd.boot.threading.Executors;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * base
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-12-16 11:39
 **/
@Slf4j
public abstract class HttpBase<H extends HttpBase> {

    public static String encode(Object object) {
        if (object == null) return null;
        return URLEncoder.encode(object.toString(), StandardCharsets.UTF_8);
    }

    protected final HttpClient httpClient;
    protected final URI uri;
    protected final LinkedHashMap<String, String> headers = new LinkedHashMap<>();
    protected long timeout = 3000;
    /** 重试状态 */
    protected int retry = 1;
    protected final Response<H> response;

    protected HttpBase(HttpClient httpClient, String url) {
        this.httpClient = httpClient;
        try {
            this.uri = new URI(url);
        } catch (URISyntaxException e) {
            throw Throw.as(e);
        }

        header(HttpHeadNameType.Content_Type, HttpHeadValueType.Application);
        header(HttpHeadNameType.Accept_Encoding, HttpHeadValueType.Gzip);
        header("user-agent", "java.org.wxd j21");
        response = new Response(this, url);
    }

    protected HttpRequest.Builder builder() {
        return HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMillis(timeout));
    }

    public Response<H> request() {
        HttpRequest.Builder builder = builder();

        builder.version(HttpClient.Version.HTTP_1_1);

        for (Map.Entry<String, String> stringEntry : headers.entrySet()) {
            builder.setHeader(stringEntry.getKey(), stringEntry.getValue());
        }

        header("connection", "close");

        HttpRequest build = builder.build();

        if (log.isDebugEnabled()) {
            log.debug(this.getClass().getSimpleName() + " " + this.response.uriPath);
            final String collect = headers.entrySet().stream()
                    .map(entry -> entry.getKey() + ":" + String.join("=", entry.getValue()))
                    .collect(Collectors.joining(", "));
            log.debug("http head：" + collect);
            if (log.isDebugEnabled() && StringUtil.notEmptyOrNull(this.response.postText)) {
                log.debug("http send：" + this.response.postText);
            }
        }

        Exception exception = null;
        for (int i = 0; i < retry; i++) {
            try {
                response.httpResponse = httpClient.send(build, HttpResponse.BodyHandlers.ofByteArray());
                return response;
            } catch (Exception e) {
                exception = e;
            }
        }
        throw Throw.as(this.getClass().getSimpleName() + " 重试次数：" + retry + ", url:" + url(), exception);
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
                        log.error("异常 {}", url(), throwable);
                    } else {
                        RuntimeException aThrow = new Throw(throwable.toString());
                        aThrow.setStackTrace(response.threadStackTrace);
                        error.accept(aThrow);
                    }
                    return response;
                });
    }

    protected void actionThrowable(Throwable throwable) {
        log.error("{} url:{}", this.getClass().getSimpleName(), uri, throwable);
        if (retry > 1)
            GlobalUtil.exception(this.getClass().getSimpleName() + " url:" + uri, throwable);
    }

    /** 读取超时 */
    public H setTimeout(long timeout) {
        this.timeout = timeout;
        return (H) this;
    }

    /** 设置重试次数 */
    public H reTry(int reTry) {
        if (reTry < 1) throw new RuntimeException("重试次数最少是1");
        this.retry = reTry;
        return (H) this;
    }

    public H header(HttpHeadNameType name, HttpHeadValueType value) {
        header(name.getValue(), value.getValue());
        return (H) this;
    }

    public H header(HttpHeadNameType name, String value) {
        header(name.getValue(), value);
        return (H) this;
    }

    public H header(String name, HttpHeadValueType value) {
        header(name, value.getValue());
        return (H) this;
    }

    public H header(String name, String value) {
        this.headers.put(name, value);
        return (H) this;
    }

    public String url() {
        return uri.toString();
    }

}
