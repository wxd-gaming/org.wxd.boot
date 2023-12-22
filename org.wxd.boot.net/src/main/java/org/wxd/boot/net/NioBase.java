package org.wxd.boot.net;

import io.netty.channel.ChannelPipeline;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于 netty 实现 通信基类
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-06-01 11:01
 **/
@Slf4j
@Getter
@Setter
@Accessors(chain = true)
public abstract class NioBase {

    protected final ReentrantLock relock = new ReentrantLock();

    protected String name;
    protected String host = null;
    protected String wanIp = null;
    protected int port;

    public abstract NioBase initBootstrap();

    protected abstract NioBase initChannel(ChannelPipeline pipeline);

}
