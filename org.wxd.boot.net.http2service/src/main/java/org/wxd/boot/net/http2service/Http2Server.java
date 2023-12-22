package org.wxd.boot.net.http2service;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.httpclient.ssl.SslContextServer;
import org.wxd.boot.httpclient.ssl.SslProtocolType;
import org.wxd.boot.net.ssl.WxOptionalSslHandler;

import javax.net.ssl.SSLContext;
import java.nio.charset.StandardCharsets;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-12-22 10:58
 **/
@Slf4j
public class Http2Server {

    public static void main(String[] args) {
        Http2Server http2Server = new Http2Server();
        http2Server.start(8443);
    }

    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    final SSLContext wxd2021 = SslContextServer.sslContext(SslProtocolType.SSLV3, "org_wxd_pkcs12.keystore", "wxd2021");

    public synchronized void start(int port) {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.option(ChannelOption.SO_BACKLOG, 1024)
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new Http2ServerInitializer());
            //
            ChannelFuture channelFuture = bootstrap.bind(port).sync();
            log.info("http2 server started on port:{}", port);
        } catch (Exception e) {
            log.error("Http2ServerBootstrap-->", e);
            close();
        }
    }

    public class Http2ServerInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel channel) throws Exception {
            ChannelPipeline pipeline = channel.pipeline();
            pipeline.addLast("sslHandler", new WxOptionalSslHandler(Http2Server.this.wxd2021));
            pipeline.addLast( new MyAppProtocolNegotiationHandler());

        }
    }

    public class TTs extends ChannelInboundHandlerAdapter {
        @Override public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            super.channelRead(ctx, msg);

            ctx.writeAndFlush("ddd".getBytes(StandardCharsets.UTF_8)).addListener(future -> ctx.disconnect());
        }
    }

    // 根据请求的协议添加对应的处理器
    public class MyAppProtocolNegotiationHandler extends ApplicationProtocolNegotiationHandler {

        public MyAppProtocolNegotiationHandler() {
            super(ApplicationProtocolNames.HTTP_2);
        }

        protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
            ChannelPipeline pipeline = ctx.pipeline();
            if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
                Http2FrameCodec http2FrameCodec = Http2FrameCodecBuilder.forServer().build();
                pipeline.addLast(http2FrameCodec);
                pipeline.addLast(new Http2ServerResponseHandler());
            } else {
                pipeline.addLast(new HttpRequestDecoder());
                pipeline.addLast(new HttpServerCodec());
                /*向客户端发送HTML5文件。主要用于支持浏览器和服务端进行WebSocket通信*/
                pipeline.addLast(new ChunkedWriteHandler());
                /*顺序必须保证*/
                pipeline.addLast(new TTs());
                pipeline.addLast(new HttpResponseEncoder());
            }
        }
    }

    public synchronized void close() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("destroy netty server thread");
    }

}

