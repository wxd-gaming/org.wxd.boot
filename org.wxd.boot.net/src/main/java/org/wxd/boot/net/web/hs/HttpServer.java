package org.wxd.boot.net.web.hs;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.DateFormatter;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.exception.Throw;
import org.wxd.boot.agent.io.FileReadUtil;
import org.wxd.boot.agent.io.FileUtil;
import org.wxd.boot.agent.zip.GzipUtil;
import org.wxd.boot.collection.ObjMap;
import org.wxd.boot.http.HttpDataAction;
import org.wxd.boot.http.HttpHeadValueType;
import org.wxd.boot.http.ssl.SslProtocolType;
import org.wxd.boot.net.NioFactory;
import org.wxd.boot.net.NioServer;
import org.wxd.boot.net.controller.MappingFactory;
import org.wxd.boot.net.handler.SocketChannelHandler;
import org.wxd.boot.str.StringUtil;
import org.wxd.boot.system.BytesUnit;
import org.wxd.boot.system.JvmUtil;
import org.wxd.boot.timer.MyClock;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-12-30 20:33
 */
@Slf4j
@Getter
@Setter
@Accessors(chain = true)
public class HttpServer extends NioServer<HttpSession> {

    /** 过期时间格式化 */
    public static SimpleDateFormat ExpiresFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
    /** 分段传输的最小字节数 */
    public static int MaxContentLength = (int) BytesUnit.Mb.toBytes(5);

    public final class HServerHandler extends SocketChannelHandler<HttpSession> {

        public HServerHandler(String name) {
            super(name, true);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            HttpSession session = NioFactory.attr(ctx, NioFactory.Session);
            if (session == null) {
                session = new HttpSession("http-server-" + HttpServer.this.getName(), ctx);
                if (!HttpServer.this.checkIPFilter(session.getIp())) {
                    session.disConnect("IP 异常");
                }
                session.setResHeaderMap(new HashMap<>(HttpServer.this.headerMap));
            }
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            try {
                HttpSession session = NioFactory.attr(ctx, NioFactory.Session);
                if (session != null) {
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
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
            HttpSession session = NioFactory.attr(ctx, NioFactory.Session);
            if (session != null) {
                response(session,
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.INTERNAL_SERVER_ERROR,
                        HttpHeadValueType.Text,
                        Throw.ofString(cause).getBytes(StandardCharsets.UTF_8)
                );
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
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            HttpSession session = NioFactory.attr(ctx, NioFactory.Session);
            if (session != null) {
                if (session.isGmSession()) {
                    return;
                }
            }
            super.userEventTriggered(ctx, evt);
        }

        @Override
        protected void channelRead0(HttpSession session, Object object) throws Exception {
//            log.debug(object.getClass().getName());
            if (object instanceof HttpRequest) {
                if (session.getRequest() == null) {
                    session.setRequest((HttpRequest) object);
                    session.setFirstReadTime(MyClock.millis());
                }
            }

            HttpMethod reqMethod = session.getRequest().method();

            try {
                if (reqMethod.equals(HttpMethod.POST)) {
                    if (object instanceof HttpContent httpContent) {
                        /*todo 只是拷贝数据，不能读取数据，缓存字节数组*/
                        int len = httpContent.content().readableBytes();
                        byte[] reqContentByteBuf = session.getReqContentByteBuf();
                        int dstIndex = 0;
                        if (reqContentByteBuf == null) {
                            /*初始化数组*/
                            reqContentByteBuf = new byte[len];
                        } else {
                            /*数组扩容*/
                            dstIndex = reqContentByteBuf.length;
                            reqContentByteBuf = Arrays.copyOf(reqContentByteBuf, reqContentByteBuf.length + len);
                        }
                        /*todo 只是拷贝数据，不能读取数据*/
                        httpContent.content().getBytes(0, reqContentByteBuf, dstIndex, len);
                        session.setReqContentByteBuf(reqContentByteBuf);
                        if (session.getHttpDecoder() != null) {
                            session.getHttpDecoder().offer(httpContent);
                        }
                        try {
                            httpContent.content().release();
                        } catch (Exception e) {
                            log.error("release() {}", session, e);
                        }
                    }
                } else if (!reqMethod.equals(HttpMethod.GET)) {
                    response(session, HttpHeadValueType.Text, NioFactory.EmptyBytes);
                    return;
                }

                if (!(object instanceof LastHttpContent)) {
                    return;
                }
                session.setLastReadTime(MyClock.millis());
                session.actionGetData();
                if (reqMethod.equals(HttpMethod.POST)) {
                    session.actionPostData();
                }
                new HttpListenerAction(HttpServer.this, session)
                        .submit();
            } catch (Throwable e) {
                log.error("{} remoteAddress：{}", HttpServer.this, session, e);
                response(session, HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR, HttpHeadValueType.Text, Throw.ofString(e).getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    /** 资源缓存，比如js css等 */
    protected boolean needCache = false;
    protected String resourcesPath;
    protected Map<String, String> headerMap = new LinkedHashMap<>();
    protected ClassLoader resourceClassLoader = this.getClass().getClassLoader();

    @Override public void open() {
        super.open();
        MappingFactory.textMappingRecord(getName())
                .forEach(v -> {
                    log.debug("http://{}:{}{}", this.getWanIp(), this.getPort(), v.path());
                });

    }

    @Override protected int idleTime() {
        return JvmUtil.getProperty(JvmUtil.Netty_Idle_Time_Http_Server, 20, Integer::valueOf);
    }

    @Override
    public HttpServer initBootstrap() {
        super.initBootstrap();
        return this;
    }

    @Override
    public HttpServer initChannel(ChannelPipeline pipeline) {
        pipeline.addLast(
                new HttpRequestDecoder(),
                new HttpServerCodec(),
                /*向客户端发送HTML5文件。主要用于支持浏览器和服务端进行WebSocket通信*/
                new ChunkedWriteHandler(),
                /*顺序必须保证*/
                new HServerHandler(this.getName()),
                new HttpResponseEncoder()
        );
        return this;
    }

    public String resourcesPath() throws IOException {
        if (StringUtil.emptyOrNull(resourcesPath)) {
            resourcesPath = FileUtil.getCanonicalPath(new File("html"));
        }
        return resourcesPath;
    }

    @Override
    public HttpServer setName(String name) {
        super.setName(name);
        return this;
    }

    @Override
    public HttpServer setHost(String host) {
        super.setHost(host);
        return this;
    }

    @Override public HttpServer setWanIp(String wanIp) {
        super.setWanIp(wanIp);
        return this;
    }

    @Override
    public HttpServer setPort(int port) {
        super.setPort(port);
        return this;
    }

    @Override public HttpServer setSslType(SslProtocolType sslType) {
        super.setSslType(sslType);
        return this;
    }

    @Override
    public HttpServer setMaxReadCount(int maxReadCount) {
        super.setMaxReadCount(maxReadCount);
        return this;
    }

    @Override public HttpServer setSslContext(SSLContext sslContext) {
        super.setSslContext(sslContext);
        return this;
    }

    @Override
    public String toString() {
        return "http-server " + this.getName() + " http://" + getWanIp() + ":" + port;
    }

    /** 提供文件下载功能 */
    public static void downloadFile(HttpSession session, File file) {
        final byte[] bytes = FileReadUtil.readBytes(file);
        downloadFile(session, file.getName(), bytes);
    }

    /** 提供文件下载功能 */
    public static void downloadFile(HttpSession session, String fileName, byte[] bytes) {
        String extendName = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        HttpHeadValueType hct = httpContentType(extendName);
        response(session, HttpVersion.HTTP_1_1, HttpResponseStatus.OK, hct, bytes, response -> {
            response.headers().add(HttpHeaderNames.CONTENT_DISPOSITION, "attachment;filename=" + HttpDataAction.urlEncoder(fileName));
        });
    }

    public static void response500(HttpSession session, String res) {
        response(session, HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR, HttpHeadValueType.Text, res.getBytes(StandardCharsets.UTF_8));
    }

    /** 关闭链接 */
    public static void response(HttpSession session, HttpHeadValueType contentType, byte[] bytes) {
        response(session, HttpVersion.HTTP_1_1, HttpResponseStatus.OK, contentType, bytes);
    }

    /** 关闭链接 */
    public static void response(HttpSession session, HttpVersion hv, HttpResponseStatus hrs, HttpHeadValueType contentType, byte[] bytes) {
        response(session, hv, hrs, contentType, bytes, null);
    }

    public static void response(HttpSession session, HttpVersion hv, HttpResponseStatus hrs, HttpHeadValueType contentType, final byte[] bytes, Consumer<HttpResponse> before) {
        try {
            session.responseOver();
            if (session.getRequest() == null) {
                session.disConnect("异常关闭的");
                return;
            }
            boolean accept_gzip = false;

            if (bytes.length > 512) {
                String accept_Encoding = session.headerOptional(HttpHeaderNames.ACCEPT_ENCODING)
                        .map(String::toLowerCase)
                        .orElse(null);

                if (accept_Encoding != null && accept_Encoding.contains("gzip")) {
                    accept_gzip = true;
                }

            }

            ByteBuf byteBuf;
            if (accept_gzip) {
                byteBuf = Unpooled.wrappedBuffer(GzipUtil.gzip(bytes));
            } else {
                byteBuf = Unpooled.wrappedBuffer(bytes);
            }

            HttpResponse response;
            int readableBytes = byteBuf.readableBytes();
            if (readableBytes > MaxContentLength) {
                response = new DefaultHttpResponse(hv, hrs);
                /*表明是分段传输*/
                response.headers().add(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
            } else {
                response = new DefaultFullHttpResponse(hv, hrs, byteBuf);

            }
            session.getResCookie().serverCookie(response.headers());
            if (session.getResHeaderMap() != null && !session.getResHeaderMap().isEmpty()) {
                for (Map.Entry<String, String> stringStringEntry : session.getResHeaderMap().entrySet()) {
                    response.headers().set(stringStringEntry.getKey(), stringStringEntry.getValue());
                }
            }

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType.getValue());
//            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, byteBuf.readableBytes());

            if (accept_gzip) {
                response.headers().set(HttpHeaderNames.CONTENT_ENCODING, "gzip");
            }

            if (before != null) {
                before.accept(response);
            }

            if (response.headers().contains(HttpHeaderNames.EXPIRES.toString())) {
                response.headers().set(HttpHeaderNames.EXPIRES.toString(), DateFormatter.format(new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2))));
            }

            if (session.isShowLog() || log.isDebugEnabled()) {
                StringBuilder stringBuilder = session.showLog();
                if (!stringBuilder.isEmpty()) {
                    stringBuilder
                            .append(";\n=============================================输出================================================")
                            .append("\n").append(new String(bytes, StandardCharsets.UTF_8))
                            .append("\n=============================================结束================================================")
                            .append("\n");
                    log.info(stringBuilder.toString());
                }
            }

            if (readableBytes > MaxContentLength) {
                ChannelFuture channelFuture = session.getChannelContext().writeAndFlush(response);
                channelFuture.addListener(new GenericFutureListener<Future<? super Void>>() {
                    @Override
                    public void operationComplete(Future<? super Void> future) throws Exception {
                        int need = Math.min(MaxContentLength, byteBuf.readableBytes());
                        if (need != 0) {
                            ByteBuf buffer = session.getChannelContext().alloc().buffer();
                            buffer.writeBytes(byteBuf, need);
                            session.getChannelContext().writeAndFlush(buffer).addListener(this);
                        } else {
                            session.getChannelContext()
                                    .writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
                                    .addListener((ChannelFutureListener) future1 -> {
                                        /*表示文件已经传输完成*/
                                        session.disConnect("分段传输完成");
                                        byteBuf.release();
                                    });

                        }
                    }
                });
            } else {
                session.setResTime(MyClock.millis());
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, byteBuf.readableBytes());
//                response.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
//                 response.headers().set(HttpHeaderNames.CONNECTION, "close");
                session.getChannelContext()
                        .writeAndFlush(response)
                        .addListener((ChannelFutureListener) future1 -> {
                            session.disConnect("正常传输完成");
                        });
            }
        } catch (Throwable throwable) {
            log.warn("response error", throwable);
        }

    }

    public static HttpHeadValueType httpContentType(String extendName) {
        switch (extendName) {
            case "htm":
            case "html":
            case "jsp":
            case "asp":
            case "aspx":
            case "xhtml":
                return HttpHeadValueType.Html;
            case "css":
            case "less":
            case "sass":
            case "scss":
                return HttpHeadValueType.CSS;
            case "ts":
            case "js":
                return HttpHeadValueType.Javascript;
            case "xml":
                return HttpHeadValueType.Xml;
            case "json":
                return HttpHeadValueType.Json;
            case "xjson":
                return HttpHeadValueType.XJson;
            case "ico":
                return HttpHeadValueType.ICO;
            case "icon":
                return HttpHeadValueType.ICON;
            case "gif":
                return HttpHeadValueType.GIF;
            case "jpg":
            case "jpe":
            case "jpeg":
                return HttpHeadValueType.JPG;
            case "png":
                return HttpHeadValueType.PNG;
            default:
                return HttpHeadValueType.OctetStream;
        }
    }

    /**
     * {@code key=value&key=value&key=value&key=value&key=value}
     */
    public static Map queryStringMap(String queryString) {
        ObjMap paramsMap = new ObjMap();
        queryStringMap(paramsMap, queryString);
        return paramsMap;
    }

    /**
     * {@code key=value&key=value&key=value&key=value&key=value}
     */
    public static void queryStringMap(Map paramsMap, String queryString) {
        if (StringUtil.emptyOrNull(queryString)) {
            return;
        }
        QueryStringDecoder queryDecoder = new QueryStringDecoder(queryString, false);
        Map<String, List<String>> uriAttributes = queryDecoder.parameters();
        for (Map.Entry<String, List<String>> attr : uriAttributes.entrySet()) {
            String get = String.join(",", attr.getValue());
            paramsMap.put(attr.getKey(), get);
        }
    }
}
