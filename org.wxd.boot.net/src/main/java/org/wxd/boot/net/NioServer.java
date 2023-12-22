package org.wxd.boot.net;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.collection.concurrent.ConcurrentHashSet;
import org.wxd.boot.httpclient.ssl.SslProtocolType;
import org.wxd.boot.net.ssl.WxOptionalSslHandler;
import org.wxd.boot.str.StringUtil;
import org.wxd.boot.system.BytesUnit;
import org.wxd.boot.system.GlobalUtil;
import org.wxd.boot.system.JvmUtil;

import javax.net.ssl.SSLContext;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2020-12-30 20:33
 */
@Slf4j
@Getter
@Setter
@Accessors(chain = true)
public abstract class NioServer<S extends Session> extends NioBase implements Runnable {

    protected ServerBootstrap bootstrap;
    protected Channel serverChannel;
    protected SslProtocolType sslType = SslProtocolType.TLSV12;
    /** 每一秒钟接受消息最大数量 */
    protected int maxReadCount = -1;
    /** ssl处理 */
    private SSLContext sslContext = null;
    /**
     * ip黑名单，坚决不允许访问，
     */
    private final ConcurrentHashSet<String> blackIPSet = new ConcurrentHashSet<>();
    /** ip白名单，如果存在此值，那么访问ip必须是列表里面的值 */
    private final ConcurrentHashSet<String> whiteIPSet = new ConcurrentHashSet<>();

    protected int idleTime() {
        int idleTime = JvmUtil.getProperty(JvmUtil.Netty_Idle_Time_Server, 20, Integer::valueOf);
        return idleTime;
    }

    @Override
    public NioServer<S> initBootstrap() {
        bootstrap = new ServerBootstrap().group(NioFactory.bossThreadGroup(), NioFactory.workThreadGroup())
                /*channel方法用来创建通道实例(NioServerSocketChannel类来实例化一个进来的链接)*/
                .channel(NioFactory.serverSocketChannelClass())
                /*方法用于设置监听套接字*/
                .option(ChannelOption.SO_BACKLOG, 8000)
                /*地址重用，socket链接断开后，立即可以被其他请求使用*/
                .option(ChannelOption.SO_REUSEADDR, true)
                /*方法用于设置和客户端链接的套接字*/
                .childOption(ChannelOption.TCP_NODELAY, true)
                /*是否启用心跳保活机机制*/
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                /*地址重用，socket链接断开后，立即可以被其他请求使用*/
                .childOption(ChannelOption.SO_REUSEADDR, true)
                /*发送缓冲区 影响 channel.isWritable()*/
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1, (int) BytesUnit.Mb.toBytes(12)))
                /*接收缓冲区，使用内存池*/
                .childOption(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(512, 2048, (int) BytesUnit.Mb.toBytes(12)))
                /*为新链接到服务器的handler分配一个新的channel。ChannelInitializer用来配置新生成的channel。(如需其他的处理，继续ch.pipeline().addLast(新匿名handler对象)即可)*/
                .childHandler(
                        new ChannelInitializer<SocketChannel>() {

                            @Override
                            public void initChannel(SocketChannel socketChannel) throws Exception {
                                ChannelPipeline pipeline = socketChannel.pipeline();
                                if (JvmUtil.getProperty(JvmUtil.Netty_Debug_Logger, false, Boolean::parseBoolean)) {
                                    pipeline.addLast("logging", new LoggingHandler("DEBUG"));// 设置log监听器，并且日志级别为debug，方便观察运行流程
                                }

                                pipeline.addFirst(new WxOptionalSslHandler(sslContext));

                                int idleTime = idleTime();
                                if (idleTime > 0) {
                                    /*设置15秒的读取空闲*/
                                    pipeline.addLast("idlehandler",
                                            new IdleStateHandler(
                                                    idleTime,
                                                    0,
                                                    0,
                                                    TimeUnit.SECONDS
                                            )
                                    );
                                }
                                NioServer.this.initChannel(pipeline);
                            }

                        }
                );
        return this;
    }

    public void open() {
        relock.lock();
        try {
            try {
                /* Bind and start to accept incoming connections*/
                this.initBootstrap();
                ChannelFuture channelFuture;
                if (null == this.getHost()
                        || "".equals(this.getHost())
                        || "*".equals(this.getHost())
//                        || "0.0.0.0".equals(this.getHost())
//                        || "0.0.0.0.0.0.0.0".equals(this.getHost())/*ipv6*/
//                        || "::".equals(this.getHost())/*ipv6*/
                ) {
                    channelFuture = bootstrap.bind(this.getPort()).sync();
                } else {
                    /*固定绑定，比如 127.0.0.1 */
                    channelFuture = bootstrap.bind(this.getHost(), this.getPort()).sync();
                }
                serverChannel = channelFuture.channel();
                log.info(this.getClass() + " " + this.toString() + " - " + this.getPort() + " 服务器已启动");
            } catch (Throwable ex) {
                log.error(this.getClass() + " " + this.toString() + " - " + this.getPort() + " 启动异常", ex);
                JvmUtil.halt(500);
            }
        } finally {
            relock.unlock();
        }
    }

    @Override public void run() {
        /*如果因为特殊原因，监听端口直接跨了，那么重新开起来*/
        relock.lock();
        try {
            if (bootstrap != null && serverChannel != null) {
                if (!serverChannel.isRegistered() && !serverChannel.isOpen()) {
                    log.error("端口监听异常了 {}", this.toString());
                    GlobalUtil.exception("端口监听异常" + this.toString(), null);
                    open();
                }
            }
        } finally {
            relock.unlock();
        }
    }

    /**
     *
     */
    public void close() {
        relock.lock();
        try {
            if (this.bootstrap != null) {
                log.info("=====服务关闭 " + this.toString() + " {start}");
                if (serverChannel != null) {
                    serverChannel.close();
                    serverChannel = null;
                }
                this.bootstrap = null;
                log.info("=====服务关闭 " + this.toString() + " {end}");
            }
        } finally {
            relock.unlock();
        }
    }

    /**
     * 一旦调用白名单设置，除了白名单一切ip都不允许访问
     */
    public NioServer<S> addWhiteIP(String... ips) {
        whiteIPSet.addAll(Arrays.asList(ips));
        return this;
    }

    /**
     * 设置黑名单，不允许访问列表
     */
    public NioServer<S> addBlackIP(String... ips) {
        blackIPSet.addAll(Arrays.asList(ips));
        return this;
    }

    /**
     * 移除白名单，如果白名单为空，表示无限制
     */
    public NioServer<S> removeWhiteIP(String... ips) {
        for (String ip : ips) {
            whiteIPSet.remove(ip);
        }
        return this;
    }

    /**
     * 移除黑名单
     *
     * @param ips
     */
    public NioServer<S> removeBlackIP(String... ips) {
        for (String ip : ips) {
            blackIPSet.remove(ip);
        }
        return this;
    }

    public boolean checkIPFilter(String ip) {
        try {
            if (StringUtil.emptyOrNull(ip)) {
                log.warn("ip地址欺诈：：：" + ip);
                return false;
            }

            if (this.getWhiteIPSet().size() > 0) {
                String[] ips = this.getWhiteIPSet().toArray(new String[this.getWhiteIPSet().size()]);
                boolean isreturn = true;
                for (String s : ips) {
                    if (ip.startsWith(s)) {
                        isreturn = false;
                        break;
                    }
                }

                if (isreturn) {
                    log.warn("不在ip地址白名单列表中：：：" + ip);
                    return false;
                }

            }

            if (this.getBlackIPSet().size() > 0) {
                String[] ips = this.getBlackIPSet().toArray(new String[this.getBlackIPSet().size()]);
                for (String s : ips) {
                    if (ip.startsWith(s)) {
                        log.warn("ip地址黑名单列表中：：：" + ip);
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            log.error("在处理ip地址验证的时候异常", e);
        }
        return false;
    }

}
