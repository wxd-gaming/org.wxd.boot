package org.wxd.boot.starter.service;

import org.wxd.boot.net.web.ws.WebSession;
import org.wxd.boot.net.web.ws.WebSocketServer;
import org.wxd.boot.starter.InjectorContext;
import org.wxd.boot.starter.WebConfig;
import org.wxd.boot.starter.i.IShutdown;
import org.wxd.boot.starter.i.IStart;

/**
 * web socket service
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-12-11 17:49
 **/
public class WsService extends WebSocketServer<WebSession> implements IStart, IShutdown {

    public WsService(WebConfig config) throws Exception {
        setName(config.getName())
                .setHost(config.getHost())
                .setWanIp(config.getWanIp())
                .setPort(config.getPort())
                .setSslType(config.sslProtocolType())
                .setSslContext(config.sslContext())
                .initBootstrap();

        if (config.getHeaders() != null && !config.getHeaders().isEmpty()) {
            for (WebConfig.Header header : config.getHeaders()) {
                getHeaderMap().put(header.getKey(), header.getValue());
            }
        }
    }

    @Override public void start(InjectorContext iocInjector) throws Exception {
        open();
    }

    @Override public void shutdown() throws Exception {
        close();
    }

}
