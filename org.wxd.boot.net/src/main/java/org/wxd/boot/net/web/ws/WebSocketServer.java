package org.wxd.boot.net.web.ws;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.function.ConsumerE2;
import org.wxd.boot.httpclient.ssl.SslProtocolType;
import org.wxd.boot.net.NioFactory;
import org.wxd.boot.net.SocketServer;
import org.wxd.boot.net.controller.MessageController;
import org.wxd.boot.net.handler.INotController;
import org.wxd.boot.net.handler.SocketChannelHandler;
import org.wxd.boot.net.util.ByteBufUtil;
import org.wxd.boot.net.web.hs.HttpServer;
import org.wxd.boot.system.BytesUnit;
import org.wxd.boot.system.JvmUtil;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 基于 netty web socket 服务
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-08-19 10:49
 **/
@Slf4j
@Getter
@Setter
@Accessors(chain = true)
public class WebSocketServer<S extends WebSession> extends SocketServer<S> {

    /** 收到 string 消息回调 */
    private ConsumerE2<S, String> onStringMessage;

    /** 资源缓存，比如js css等 */
    private Map<String, String> headerMap = new LinkedHashMap<>();

    protected WebSocketServer() {
    }

    @Override protected int idleTime() {
        int idleTime = JvmUtil.getProperty(JvmUtil.Netty_Idle_Time_Ws_Server, 20, Integer::valueOf);
        return idleTime;
    }

    @Override
    public WebSocketServer<S> initBootstrap() {
        super.initBootstrap();
        return this;
    }

    @Override
    protected WebSocketServer<S> initChannel(ChannelPipeline pipeline) {
        pipeline
                /*设置解码器*/
                .addLast(
                        new HttpServerCodec(),
                        new HttpObjectAggregator((int) BytesUnit.Mb.toBytes(64)),/*接受完整的http消息 64mb*/
//                        new WebSocketServerCompressionHandler(),
                        new ChunkedWriteHandler(),/*用于大数据的分区传输*/
                        new WebSocketServerChannelHandler(this.toString())/*自定义的业务handler*/
                );
        return this;
    }

    @Override
    public S newSession(String name, ChannelHandlerContext ctx) {
        return (S) new WebSession(name, ctx);
    }

    public WebSocketServer<S> writeAll(String msg) {
        for (S value : getAllSessionMap().values()) {
            value.write(msg);
        }
        return this;
    }

    @Override
    public WebSocketServer<S> setName(String name) {
        super.setName(name);
        return this;
    }

    @Override
    public WebSocketServer<S> setHost(String host) {
        super.setHost(host);
        return this;
    }

    @Override public WebSocketServer<S> setWanIp(String wanIp) {
        super.setWanIp(wanIp);
        return this;
    }

    @Override
    public WebSocketServer<S> setPort(int port) {
        super.setPort(port);
        return this;
    }

    @Override public WebSocketServer<S> setSslType(SslProtocolType sslType) {
        super.setSslType(sslType);
        return this;
    }

    @Override public WebSocketServer<S> setMaxReadCount(int maxReadCount) {
        super.setMaxReadCount(maxReadCount);
        return this;
    }

    @Override public WebSocketServer<S> setSslContext(SSLContext sslContext) {
        super.setSslContext(sslContext);
        return this;
    }

    @Override
    public WebSocketServer<S> setOnOpenSession(Consumer<S> onOpenSession) {
        super.setOnOpenSession(onOpenSession);
        return this;
    }

    @Override
    public WebSocketServer<S> setOnCloseSession(Consumer<S> onCloseSession) {
        super.setOnCloseSession(onCloseSession);
        return this;
    }

    @Override
    public WebSocketServer<S> setOnNotController(INotController<S> onNotController) {
        super.setOnNotController(onNotController);
        return this;
    }

    @Override
    public WebSocketServer<S> msgExecutorBefore(Predicate<MessageController> messageExecutorBefore) {
        super.msgExecutorBefore(messageExecutorBefore);
        return this;
    }

    public String toString(String host, String url) {
        return "ws://" + host + ":" + port + "/" + url;
    }

    @Override
    public String toString() {
        return "web-server " + this.getName();
    }

    /**
     * web socket server
     */
    class WebSocketServerChannelHandler extends SocketChannelHandler<S> {

        WebSocketServerHandshakerFactory wsFactory;

        WebSocketServerHandshaker handshaker = null;

        public WebSocketServerChannelHandler(String name) {
            super(name, true);

            WebSocketDecoderConfig build = WebSocketDecoderConfig.newBuilder()
                    .maxFramePayloadLength((int) BytesUnit.Kb.toBytes(64))/*设置接收数据大小限制*/
                    .build();
            wsFactory = new WebSocketServerHandshakerFactory(
                    WebSocketServer.this.toString(),
                    null,
                    build
            );
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            S session = NioFactory.attr(ctx, NioFactory.Session);
            if (session == null) {
                session = newSession(this.name, ctx);
                if (!checkIPFilter(session.getIp())) {
                    session.disConnect("IP 异常");
                }
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
                    session.disConnect("链接断开");
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

        @Override
        protected void channelRead0(S session, Object msg) {
            if (msg instanceof FullHttpRequest) {
                // 以http请求形式接入，但是走的是websocket
                handleHttpRequest(session, (FullHttpRequest) msg);
            } else if (msg instanceof WebSocketFrame) {
                // 处理websocket客户端的消息
                handlerWebSocketFrame(session, (WebSocketFrame) msg);
            }
        }

        protected void handlerWebSocketFrame(S session, WebSocketFrame frame) {
            try {
                if (frame instanceof CloseWebSocketFrame closeWebSocketFrame /*判断是否关闭链路的指令*/) {
                    handshaker.close(session.getChannelContext().channel(), closeWebSocketFrame.retain());
                } else if (frame instanceof PingWebSocketFrame/*ping 消息*/) {
                    session.getChannelContext().channel().write(new PongWebSocketFrame(frame.content().retain()));
                } else if (frame instanceof BinaryWebSocketFrame binaryWebSocketFrame) {/*二进制数据*/
                    ByteBuf byteBuf = Unpooled.wrappedBuffer(binaryWebSocketFrame.content());
                    read(session, byteBuf);
                    session.checkReadCount(WebSocketServer.this.maxReadCount);
                } else if (frame instanceof TextWebSocketFrame textWebSocketFrame) {/*文本数据*/
                    String request = textWebSocketFrame.text();
                    session.checkReadTime();
                    session.addReadCount();
                    session.checkReadCount(WebSocketServer.this.maxReadCount);
                    if (WebSocketServer.this.onStringMessage != null) {
                        WebSocketServer.this.onStringMessage.accept(session, request);
                    } else {
                        log.debug("当前不接受文本消息：{}, {}", session, request);
                    }
                }
            } catch (Throwable e) {
                log.warn("处理消息异常", e);
            }
        }

        /**
         * 唯一的一次http请求，用于创建websocket
         */
        private void handleHttpRequest(S session, FullHttpRequest httpRequest) {
            try {
                URI uri = new URI(httpRequest.uri());
                String path = uri.getPath();

                String cookieString = httpRequest.headers().get(HttpHeaderNames.COOKIE);

                if (log.isInfoEnabled()) {
                    StringBuilder stringBuilder = new StringBuilder();

                    log.info(
                            stringBuilder
                                    .append("\n")
                                    .append("=============================================================================================").append("\n")
                                    .append("Host：Web Socket ").append(session.getRemoteAddress()).append(uri).append(";\n")
                                    .append("User-Agent：").append(httpRequest.headers().get(HttpHeaderNames.USER_AGENT)).append(";\n")
                                    .append("Accept-Encoding：").append(httpRequest.headers().get(HttpHeaderNames.ACCEPT_ENCODING)).append(";\n")
                                    .append(HttpHeaderNames.COOKIE).append("：").append(cookieString).append(";\n")
                                    .append("=============================================================================================")
                                    .toString()
                    );
                }

                if (!httpRequest.decoderResult().isSuccess()
                        || (!"websocket".equalsIgnoreCase(httpRequest.headers().get("Upgrade")))/*判别必须websocket不能是get或者post*/) {
                    // 若不是websocket方式，则创建BAD_REQUEST的req，返回给客户端
                    log.warn("收到的监听不正确，拒绝 请求 -> " + path);
                } else {
                    String accept_Encoding = httpRequest.headers().get(HttpHeaderNames.ACCEPT_ENCODING);
                    if (accept_Encoding != null && accept_Encoding.contains("gzip")) {
                        session.setAccept_gzip(true);
                    }
                    String content_Encoding = httpRequest.headers().get(HttpHeaderNames.CONTENT_ENCODING);
                    if (content_Encoding != null && content_Encoding.contains("gzip")) {
                        session.setContent_gzip(true);
                    }
                    session.setRequest(httpRequest);
                    session.getReqCookies().decodeServerCookie(cookieString);
                    HttpServer.queryStringMap(session.getReqParams(), uri.getQuery());
                    handshaker = wsFactory.newHandshaker(httpRequest);
                    final Channel channel = session.getChannelContext().channel();
                    if (handshaker != null) {
                        /*todo 可以在这里设置回复客户端的 header参数 */
                        handshaker.handshake(channel, httpRequest, (HttpHeaders) null, channel.newPromise());
                        /*验证通过才能用*/
                        openSession(session);
                        log.info("WebSocket Server 握手成功 {}", session);
                        return;
                    }
                }
            } catch (Exception e) {
                log.warn("解析异常", e);
            }
            sendHttpResponse(session, httpRequest, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
        }

        /**
         * 拒绝不合法的请求，并返回错误信息
         */
        private void sendHttpResponse(S session, FullHttpRequest req, DefaultFullHttpResponse response) {

            if (WebSocketServer.this.headerMap != null && !WebSocketServer.this.headerMap.isEmpty()) {
                for (Map.Entry<String, String> entry : WebSocketServer.this.headerMap.entrySet()) {
                    response.headers().set(entry.getKey(), entry.getValue());
                }
            }

            // 返回应答给客户端
            if (response.status().code() != 200) {
                ByteBuf buf = Unpooled.copiedBuffer(response.status().toString(), CharsetUtil.UTF_8);
                response.content().writeBytes(buf);
                ByteBufUtil.release(buf);
            }
            ChannelFuture channelFuture = session.getChannelContext().writeAndFlush(response);
            // 如果是非Keep-Alive，关闭链接
            if (response.status().code() != 200 || req == null || !HttpUtil.isKeepAlive(req)) {
                channelFuture.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

}
