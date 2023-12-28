package org.wxd.boot.starter;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.simpleframework.xml.Element;
import org.wxd.boot.http.ssl.SslContextServer;
import org.wxd.boot.http.ssl.SslProtocolType;
import org.wxd.boot.lang.ObjectBase;
import org.wxd.boot.str.StringUtil;

import javax.net.ssl.SSLContext;
import java.io.Serializable;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-09-30 09:33
 **/
@Getter
@Setter
@Accessors(chain = true)
public class TcpConfig extends ObjectBase implements Serializable {

    @Element(required = false)
    private String name = "";
    @Element(required = false)
    private String host = "";
    @Element(required = false)
    private String wanIp = "0.0.0.0";
    @Element(required = false)
    private int port = 0;
    @Element(required = false)
    private String sslProtocolType = null;
    @Element(required = false)
    private String jks = "org_wxd_pkcs12.keystore";
    @Element(required = false)
    private String jksPwd = "wxd2021";
    @Element(required = false)
    private String serviceClassName = null;

    public SslProtocolType sslProtocolType() throws Exception {
        if (StringUtil.notEmptyOrNull(sslProtocolType)) {
            return SslProtocolType.of(sslProtocolType);
        }
        return SslProtocolType.TLSV12;
    }

    public SSLContext sslContext() throws Exception {
        return SslContextServer.sslContext(sslProtocolType(), jks, jksPwd);
    }

}
