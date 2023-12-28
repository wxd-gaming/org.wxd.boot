package org.wxd.boot.httpclient.url;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 基于 HttpURLConnection 信息请求
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-01-05 12:55
 **/
@Getter
@Setter
@Accessors(chain = true)
public class Get extends HttpBase<Get> {

    protected Get(String uriPath) {
        super(uriPath);
        this.reqHttpMethod = "GET";
    }
}
