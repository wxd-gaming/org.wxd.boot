package org.wxd.boot.httpclient.jdk;

import lombok.extern.slf4j.Slf4j;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;

/**
 * 基于 java 原生的http协议支持
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-10-13 12:22
 **/
@Slf4j
public class Get extends HttpBase<Get> {

    Get(HttpClient httpClient, String url) {
        super(httpClient, url);
    }

    @Override protected HttpRequest.Builder builder() {
        return super.builder().GET();
    }

}
