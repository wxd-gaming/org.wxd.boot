package org.wxd.boot.httpclient.jdk;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.zip.GzipUtil;
import org.wxd.boot.httpclient.HttpHeadNameType;
import org.wxd.boot.str.StringUtil;

import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基于 java 原生的http 信息请求
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-11-15 12:34
 **/
@Slf4j
@Getter
public final class Response<H extends HttpBase> {

    final H httpBase;
    final String uriPath;
    String postText = null;
    StackTraceElement[] threadStackTrace = null;
    HttpResponse<byte[]> httpResponse;

    Response(H httpBase, String uriPath) {
        this.httpBase = httpBase;
        this.uriPath = uriPath;
    }

    public String getHeader(String header) {
        return this.httpResponse.headers().firstValue(header).orElse("");
    }

    public Map<String, List<String>> getHeaders() {
        return this.httpResponse.headers().map();
    }

    public int responseCode() {
        return httpResponse.statusCode();
    }

    public byte[] body() {
        byte[] body = httpResponse.body();
        if ("gzip".equalsIgnoreCase(httpResponse.headers().firstValue(HttpHeadNameType.Content_Encoding.getValue()).orElse(""))) {
            return GzipUtil.unGZip(body);
        }
        return body;
    }

    public String bodyString() {
        return bodyString(StandardCharsets.UTF_8);
    }

    public String bodyString(Charset charset) {
        return new String(body(), charset);
    }

    public String bodyUnicodeDecodeString() {
        return StringUtil.unicodeDecode(bodyString());
    }

    public Response<H> logDebug() {
        log.debug("res: {} {}", bodyString(), this.toString());
        return this;
    }

    public Response<H> logInfo() {
        log.info("res: {} {}", bodyString(), this.toString());
        return this;
    }

    public Response<H> systemOut() {
        System.out.println("res: " + bodyString() + " " + this.toString());
        return this;
    }

    @Override public String toString() {
        return httpBase.getClass().getSimpleName() + " url: " + uriPath + Optional.ofNullable(postText).map(v -> ", postText: " + postText).orElse("");
    }
}
