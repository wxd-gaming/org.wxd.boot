package org.wxd.boot.httpclient.jdk;

import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.http.HttpHeadNameType;
import org.wxd.boot.http.HttpHeadValueType;
import org.wxd.boot.lang.SyncJson;
import org.wxd.boot.publisher.Mono;
import org.wxd.boot.str.StringUtil;
import org.wxd.boot.system.GlobalUtil;

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
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 基于 java 原生的http
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
    protected StackTraceElement[] stackTraceElements;
    protected final CompletableFuture<Response<H>> responseCompletableFuture;
    protected final Mono<Response<H>> mono;

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

        responseCompletableFuture = new CompletableFuture<>();
        mono = new Mono<>(responseCompletableFuture);
    }

    protected HttpRequest.Builder builder() {
        return HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMillis(timeout));
    }

    public Mono<Response<H>> async() {
        return sendAsync(3);
    }

    public Mono<String> asyncString() {
        return sendAsync(3).map(httpResponse -> new String(httpResponse.body(), StandardCharsets.UTF_8));
    }

    public void asyncString(Consumer<String> consumer) {
        sendAsync(3).subscribe(httpResponse -> consumer.accept(new String(httpResponse.body(), StandardCharsets.UTF_8)));
    }

    public Mono<SyncJson> asyncSyncJson() {
        return sendAsync(3).map(httpResponse -> SyncJson.parse(new String(httpResponse.body(), StandardCharsets.UTF_8)));
    }

    public void asyncSyncJson(Consumer<SyncJson> consumer) {
        sendAsync(3)
                .subscribe(httpResponse -> consumer.accept(SyncJson.parse(new String(httpResponse.body(), StandardCharsets.UTF_8))))
                .onError(this::actionThrowable);
    }

    Mono<Response<H>> sendAsync(int stackTraceIndex) {

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        stackTraceElements = new StackTraceElement[stackTrace.length - stackTraceIndex];
        System.arraycopy(stackTrace, stackTraceIndex, stackTraceElements, 0, stackTraceElements.length);

        HttpRequest.Builder builder = builder();
        builder.version(HttpClient.Version.HTTP_1_1);
        for (Map.Entry<String, String> stringEntry : headers.entrySet()) {
            builder.setHeader(stringEntry.getKey(), stringEntry.getValue());
        }
        header("connection", "close");
        HttpRequest httpRequest = builder.build();

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

        action(httpRequest, 1);
        return mono;
    }

    void action(HttpRequest httpRequest, int action) {
        httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofByteArray())
                .whenComplete((httpResponse, throwable) -> {
                    if (throwable != null) {
                        if (action < retry) {
                            action(httpRequest, action + 1);
                        } else {
                            RuntimeException runtimeException = Throw.as(HttpBase.this.toString() + ", 重试：" + action, throwable);
                            runtimeException.setStackTrace(stackTraceElements);
                            this.responseCompletableFuture.completeExceptionally(runtimeException);
                        }
                    } else {
                        response.httpResponse = httpResponse;
                        this.responseCompletableFuture.complete(response);
                    }
                });
    }

    protected void actionThrowable(Throwable throwable) {
        log.error("{} url:{}", this.getClass().getSimpleName(), uri, throwable);
        if (retry > 1)
            GlobalUtil.exception(this.getClass().getSimpleName() + " url:" + uri, throwable);
    }

    /** 读取超时 */
    public H readTimeout(long timeout) {
        this.timeout = timeout;
        return (H) this;
    }

    /** 设置重试次数 */
    public H retry(int retry) {
        if (retry < 1) throw new RuntimeException("重试次数最少是1");
        this.retry = retry;
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
