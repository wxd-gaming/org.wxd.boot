package org.wxd.boot.net.web.ws;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.agent.function.ConsumerE2;
import org.wxd.boot.http.ssl.SslContextClient;
import org.wxd.boot.http.ssl.SslProtocolType;
import org.wxd.boot.net.NioClient;
import org.wxd.boot.net.NioFactory;
import org.wxd.boot.net.auth.IAuth;
import org.wxd.boot.net.auth.SignConfig;
import org.wxd.boot.net.handler.INotController;
import org.wxd.boot.net.handler.SocketChannelHandler;
import org.wxd.boot.net.ssl.WxSslHandler;
import org.wxd.boot.net.web.CookiePack;
import org.wxd.boot.system.BytesUnit;

import javax.net.ssl.SSLEngine;
import java.net.URI;
import java.util.function.Consumer;

/**
 * 客户端链接管理器
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-12-25 12:20
 */
@Slf4j
@Getter
@Setter
@Accessors(chain = true)
public class WebSocketClient<S extends WebSession> extends NioClient<S> {

    protected SslProtocolType sslProtocolType = SslProtocolType.SSL;
    /** 包含的http head参数 */
    protected final HttpHeaders httpHeaders = new DefaultHttpHeaders();
    /** http协议的cookie参数 */
    protected final CookiePack cookiePack = new CookiePack();

    protected URI uri;
    /** url监听后缀 */
    protected String urlSuffix;
    protected boolean ssl;

    private ConsumerE2<S, String> onStringMessage;

    public static void main(String[] args) throws Exception {
        WebSocketClient<WebSession> client = new WebSocketClient<>()
                .setName("post_log_client")
                .setSsl(true).setHost("192.168.30.254").setPort(19001)
                .addCookie(HttpHeaderNames.AUTHORIZATION.toString(), SignConfig.get().optByUser("root").map(IAuth::getToken).orElse(""))
                .initBootstrap();

        client.connect();

        try {
            Thread.sleep(2000);
        } catch (Exception e) {
        }
    }

    @Override
    public S newSession(String name, ChannelHandlerContext ctx) {
        return (S) new WebSession(name, ctx);
    }

    @Override public WebSocketClient<S> setOnOpen(Consumer<S> onOpen) {
        super.setOnOpen(onOpen);
        return this;
    }

    @Override public WebSocketClient<S> setOnClose(Consumer<S> onClose) {
        super.setOnClose(onClose);
        return this;
    }

    @Override
    public WebSocketClient<S> initBootstrap() {
        this.connectTimeOut = 1500;
        String ws = "ws";
        if (isSsl()) {
            ws += "s";
        }
        ws += "://" + host + ":" + port + "/" + urlSuffix;
        try {
            uri = new URI(ws);
        } catch (Exception e) {
            throw Throw.as(e);
        }
        httpHeaders.set(HttpHeaderNames.USER_AGENT, "java.org.wxd v1");
        super.initBootstrap();
        return this;
    }

    @Override
    protected WebSocketClient<S> initChannel(ChannelPipeline pipeline) {
        if (this.isSsl()) {
            SSLEngine sslEngine = SslContextClient.sslContext(sslProtocolType).createSSLEngine();
            sslEngine.setUseClientMode(true);
            sslEngine.setNeedClientAuth(false);
            pipeline.addFirst("sslhandler", new WxSslHandler(sslEngine));
        }

        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                uri,
                WebSocketVersion.V13,
                null,
                false,
                httpHeaders,
                (int) BytesUnit.Mb.toBytes(64)/*64 mb*/
        );

        pipeline.addLast(
                new HttpClientCodec(),/*将请求与应答消息编码或者解码为HTTP消息*/
                new HttpObjectAggregator((int) BytesUnit.Mb.toBytes(64)),/*接受完整的http消息 64mb*/
//                WebSocketClientCompressionHandler.INSTANCE,
                new ChunkedWriteHandler(),/*用于大数据的分区传输*/
                new WSCSocketChannelHandler(this.toString(), handshaker)/*自定义的业务handler*/
        );

        return this;
    }

    public void connect() {
        this.connect(getHost(), getPort());
    }

    public WebSocketClient<S> write(String msg) {
        for (S value : getAllSessionMap().values()) {
            value.write(msg);
        }
        return this;
    }

    public WebSocketClient<S> addCookie(String cookieKey, String cookieValue) {
        this.cookiePack.addCookie(cookieKey, cookieValue);
        this.cookiePack.clientCookie(this.httpHeaders);
        return this;
    }

    @Override
    public WebSocketClient<S> setName(String name) {
        super.setName(name);
        return this;
    }

    @Override
    public WebSocketClient<S> setHost(String host) {
        super.setHost(host);
        return this;
    }

    @Override public WebSocketClient<S> setWanIp(String wanIp) {
        super.setWanIp(wanIp);
        return this;
    }

    @Override
    public WebSocketClient<S> setPort(int port) {
        super.setPort(port);
        return this;
    }

    @Override
    public WebSocketClient<S> setDefaultSessionSize(int defaultSessionSize) {
        super.setDefaultSessionSize(defaultSessionSize);
        return this;
    }

    @Override
    public WebSocketClient<S> setConnectTimeOut(int connectTimeOut) {
        super.setConnectTimeOut(connectTimeOut);
        return this;
    }

    @Override
    public WebSocketClient<S> setOnNotController(INotController<S> onNotController) {
        super.setOnNotController(onNotController);
        return this;
    }

    @Override
    public String toString() {
        return super.toString() + " " + (this.ssl ? "wss " : "ws ");
    }

    class WSCSocketChannelHandler extends SocketChannelHandler<S> {

        private WebSocketClientHandshaker webSocketClientHandshaker;

        public WSCSocketChannelHandler(String name, WebSocketClientHandshaker webSocketClientHandshaker) {
            super(name, true);
            this.webSocketClientHandshaker = webSocketClientHandshaker;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            S session = NioFactory.attr(ctx, NioFactory.Session);
            if (session == null) {
                Channel channel = ctx.channel();
                // 阻塞等待是否握手成功
                this.webSocketClientHandshaker.handshake(channel);
                session = newSession(this.name, ctx);
            }
        }

        /**
         * 断开链接后
         *
         * @param ctx
         * @throws Exception
         */
        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            try {
                S session = NioFactory.attr(ctx, NioFactory.Session);
                if (session != null) {
                    closeSession(session);
                    session.disConnect("链接中断 Unregistered");
                } else {
                    try {
                        ctx.disconnect();
                    } catch (Exception e) {
                    }
                    try {
                        ctx.close();
                    } catch (Exception e) {
                    }
                }
            } finally {
                super.channelUnregistered(ctx);
            }
        }

        /**
         * 收到消息
         */
        @Override
        protected void channelRead0(S session, Object msg) {
            if (msg instanceof FullHttpResponse response) {
                if (!this.webSocketClientHandshaker.isHandshakeComplete()) {
                    try {
                        // 握手协议返回，设置结束握手
                        this.webSocketClientHandshaker.finishHandshake(session.getChannelContext().channel(), response);
                        /*握手成功才会回调*/
                        openSession(session);
                        log.info("握手成功 connected! {}", session);
                    } catch (WebSocketHandshakeException e) {
                        String errorMsg = String.format("Web Socket Client 握手异常 to connect,status:%s,reason:%s", response.status(), response.content().toString(CharsetUtil.UTF_8));
                        log.error(errorMsg);
                    }
                }
            } else {
                if (msg instanceof WebSocketFrame frame) {
                    try {
                        if (frame instanceof TextWebSocketFrame textFrame) {
                            session.checkReadTime();
                            if (WebSocketClient.this.onStringMessage != null) {
                                WebSocketClient.this.onStringMessage.accept(session, textFrame.text());
                            } else {
                                log.debug("当前不接受文本消息：{}, {}", session, textFrame.text());
                            }
                        } else if (frame instanceof BinaryWebSocketFrame binaryFrame) {
                            ByteBuf byteBuf = Unpooled.wrappedBuffer(binaryFrame.content());
                            read(session, byteBuf);
                        } else if (frame instanceof CloseWebSocketFrame) {
                            session.disConnect("CloseWebSocketFrame");
                        }
                    } catch (Throwable e) {
                        log.warn("处理消息异常", e);
                    }
                } else {
                    log.info(msg.toString());
                }
            }
        }
    }

}

