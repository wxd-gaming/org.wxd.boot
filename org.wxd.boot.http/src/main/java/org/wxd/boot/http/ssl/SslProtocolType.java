package org.wxd.boot.http.ssl;


import org.wxd.boot.collection.OfMap;

import java.io.Serializable;
import java.util.Map;

/**
 * ssl版本协议
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-01-11 14:30
 **/
public enum SslProtocolType implements Serializable {
    /** SSL */
    SSL("SSL"),
    /** SSLV1 */
    SSLV1("SSLV1"),
    /** SSLV2 */
    SSLV2("SSLV2"),
    /** SSLV3 */
    SSLV3("SSLV3"),
    /** TLS */
    TLS("TLS"),
    /** TLSv1 */
    TLSV1("TLSv1"),
    /** TLSv1.2 */
    TLSV12("TLSv1.2");

    private static final Map<String, SslProtocolType> Static_Map = OfMap.asMap(SslProtocolType::getTypeName, SslProtocolType.values());

    public static SslProtocolType of(String source) {
        return Static_Map.get(source);
    }

    private final String typeStr;

    SslProtocolType(String typeStr) {
        this.typeStr = typeStr;
    }

    public String getTypeName() {
        return typeStr;
    }


}
