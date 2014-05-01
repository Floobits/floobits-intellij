package floobits.common;

import floobits.common.handlers.BaseHandler;
import floobits.utilities.Flog;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;

import java.net.ConnectException;
import java.util.concurrent.TimeUnit;

/**
 * Created by kans on 5/1/14.
 */

public class FlooClientHandler extends SimpleChannelInboundHandler<Object> {
    private long startTime = -1;
    private BaseHandler handler;
    private NettyFlooConn nettyFlooConn;

    public FlooClientHandler(BaseHandler handler, NettyFlooConn nettyFlooConn) {
        this.handler = handler;
        this.nettyFlooConn = nettyFlooConn;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (startTime < 0) {
            startTime = System.currentTimeMillis();
        }
        Flog.log("Connected to: " + ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Discard received data
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            // Do something with msg
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!(evt instanceof IdleStateEvent)) {
            return;
        }

        IdleStateEvent e = (IdleStateEvent) evt;
        if (e.state() == IdleState.READER_IDLE) {
            // The connection was OK but there was no traffic for last period.
            Flog.log("Disconnecting due to no inbound traffic");
            ctx.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Flog.log("Disconnected from: " + ctx.channel().remoteAddress());
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx)
            throws Exception {
        Flog.log("Sleeping for: " + nettyFlooConn.RECONNECT_DELAY + 's');

        final EventLoop loop = ctx.channel().eventLoop();
        loop.schedule(new Runnable() {
            @Override
            public void run() {
                Flog.log("Reconnecting to: " + ctx.channel().remoteAddress());
                nettyFlooConn.connect();
            }
        }, nettyFlooConn.RECONNECT_DELAY, TimeUnit.SECONDS);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof ConnectException) {
            startTime = -1;
            Flog.log("Failed to connect: " + cause.getMessage());
        }
        cause.printStackTrace();
        ctx.close();
    }
}!