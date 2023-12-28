package org.wxd.boot.httpclient.jdk;

import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.str.StringUtil;
import org.wxd.boot.system.GlobalUtil;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于 java 原生的http协议支持
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-10-13 12:22
 **/
@Slf4j
public class PostMulti extends HttpBase<PostMulti> {

    Map<Object, Object> params = new LinkedHashMap<>();

    PostMulti(HttpClient httpClient, String url) {
        super(httpClient, url);
    }

    public PostMulti putParams(Object key, Object value) {
        params.put(key, value);
        return this;
    }

    public PostMulti putParams(Map map) {
        Map<Object, Object> tmp = (Map<Object, Object>) map;
        for (Map.Entry<Object, Object> entry : tmp.entrySet()) {
            putParams(entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Override protected HttpRequest.Builder builder() {
        HttpRequest.Builder builder = super.builder();
        response.postText = ofFormData();
        if (StringUtil.notEmptyOrNull(response.postText)) {
            builder.POST(HttpRequest.BodyPublishers.ofString(response.postText));
        } else {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        }
        return builder;
    }

    String ofFormData() {
        var builder = new StringBuilder();
        for (Map.Entry<Object, Object> entry : params.entrySet()) {
            if (!builder.isEmpty()) {
                builder.append("&");
            }
            builder.append(encode(entry.getKey())).append("=").append(encode(entry.getValue()));
        }
        return builder.toString();
    }

}
