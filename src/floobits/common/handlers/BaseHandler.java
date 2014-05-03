package floobits.common.handlers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.project.Project;
import floobits.FlooContext;
import floobits.common.FlooUrl;
import floobits.common.NettyFlooConn;
import floobits.utilities.Flog;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;

import java.net.ConnectException;
import java.util.concurrent.TimeUnit;

@ChannelHandler.Sharable
abstract public class BaseHandler extends SimpleChannelInboundHandler<String> {
    public FlooUrl url;
    public boolean isJoined = false;
    protected NettyFlooConn conn;
    public FlooContext context;

    public BaseHandler(FlooContext context) {
        this.context = context;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Flog.log("Connected to: " + ctx.channel().remoteAddress());
        on_connect();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        try {
            JsonObject obj = (JsonObject)new JsonParser().parse(msg);
            JsonElement name = obj.get("name");
            if (name == null) {
                Flog.warn("No name for receive, ignoring");
                return;
            }
            String requestName = name.getAsString();
            on_data(requestName, obj);
            Flog.log(msg);
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
        Flog.log("Sleeping for: " + conn.RECONNECT_DELAY + 's');

        final EventLoop loop = ctx.channel().eventLoop();
        loop.schedule(new Runnable() {
            @Override
            public void run() {
                Flog.log("Reconnecting to: " + ctx.channel().remoteAddress());
                conn.reconnect();
            }
        }, conn.RECONNECT_DELAY, TimeUnit.SECONDS);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof ConnectException) {
            Flog.log("Failed to connect: " + cause.getMessage());
        }
        cause.printStackTrace();
        ctx.close();
    }
    public abstract void on_data(String name, JsonObject obj);
    public abstract void on_connect();

    public Project getProject() {
        return context.project;
    }

    public FlooUrl getUrl() {
        return url;
    }

    public abstract void go();

    public void shutdown() {
        if (conn != null) {
            conn.shutdown();
            conn = null;
        }
        isJoined = false;
    }
}
