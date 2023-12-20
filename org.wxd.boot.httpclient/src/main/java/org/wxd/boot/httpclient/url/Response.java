package org.wxd.boot.httpclient.url;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.str.StringUtil;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基于 HttpURLConnection 信息请求
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-11-15 12:34
 **/
@Slf4j
@Getter
public final class Response<H extends HttpBase> {

    final H httpBase;
    final String uriPath;
    HttpURLConnection urlConnection;
    String postText = null;
    StackTraceElement[] threadStackTrace = null;
    byte[] bodys = null;

    protected Response(H httpBase, String uriPath) {
        this.httpBase = httpBase;
        this.uriPath = uriPath;
    }


    public int responseCode() {
        try {
            return this.urlConnection.getResponseCode();
        } catch (IOException e) {
            throw Throw.as(this.toString(), e);
        }
    }

    public String getHeader(String header) {
        return this.urlConnection.getHeaderField(header);
    }

    public Map<String, List<String>> getHeaders() {
        return this.urlConnection.getHeaderFields();
    }

    public String bodyString() {
        return bodyString(StandardCharsets.UTF_8);
    }

    public String bodyString(Charset charsetName) {
        return new String(bodys, charsetName);
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
