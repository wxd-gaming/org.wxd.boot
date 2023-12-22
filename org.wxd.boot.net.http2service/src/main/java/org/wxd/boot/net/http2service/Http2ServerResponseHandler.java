package org.wxd.boot.net.http2service;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.*;
import io.netty.util.CharsetUtil;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-12-22 10:57
 **/
public class Http2ServerResponseHandler extends ChannelDuplexHandler {

    static final ByteBuf RESPONSE_BYTES = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello World", CharsetUtil.UTF_8));

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2HeadersFrame msgHeader) {

        } else if (msg instanceof Http2DataFrame http2DataFrame) {
            if (http2DataFrame.isEndStream()) {

                ByteBuf content = ctx.alloc().buffer();
                content.writeBytes(RESPONSE_BYTES.duplicate());

                Http2Headers headers = new DefaultHttp2Headers().status(HttpResponseStatus.OK.codeAsText());
                /*原路返回*/
                ctx.write(new DefaultHttp2HeadersFrame(headers).stream(http2DataFrame.stream()));
                /*原路返回*/
                ctx.write(new DefaultHttp2DataFrame(content, true).stream(http2DataFrame.stream()));
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }

}

