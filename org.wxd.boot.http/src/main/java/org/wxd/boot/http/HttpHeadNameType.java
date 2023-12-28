package org.wxd.boot.http;

import lombok.Getter;

/**
 * http 协议类型
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-12-30 20:33
 */
@Getter
public enum HttpHeadNameType {
    Accept_Encoding("accept-encoding"),
    /** 服务器返回数据是否gzip */
    Content_Encoding("content-encoding"),
    Content_Type("content-type"),
    Connection("connection"),
    ;
    final String value;

    HttpHeadNameType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

}

