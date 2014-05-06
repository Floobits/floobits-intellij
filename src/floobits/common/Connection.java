package floobits.common;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import floobits.FlooContext;
import floobits.common.handlers.BaseHandler;
import floobits.utilities.Flog;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.Serializable;
import java.net.ConnectException;

@ChannelHandler.Sharable
public class Connection extends SimpleChannelInboundHandler<String> {
    private class FlooChannelInitializer extends ChannelInitializer<SocketChannel> {
        private Connection connection;

        private FlooChannelInitializer(Connection connection) {
            this.connection = connection;
        }

        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            SSLContext sslContext = Utils.createSSLContext();
            SSLEngine engine = sslContext.createSSLEngine();
            engine.setUseClientMode(true);
            pipeline.addLast("ssl", new SslHandler(engine));
            pipeline.addLast("framer", new LineBasedFrameDecoder(1000 * 1000 * 10, true, false));
            pipeline.addLast("decoder", new StringDecoder(CharsetUtil.UTF_8));
            pipeline.addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));
            pipeline.addLast("handler", connection);
        }
    }
    private final BaseHandler handler;
    private final FlooContext context;
    protected Channel channel;
    private Integer MAX_RETRIES = 20;
    private Integer INITIAL_RECONNECT_DELAY = 500;
    protected volatile Integer retries = MAX_RETRIES;
    protected Integer delay = INITIAL_RECONNECT_DELAY;

    public Connection(final BaseHandler handler){
        this.handler = handler;
        this.context = handler.context;
    }

    public void start() {
        connect();
    }

    public void write(Serializable obj) {
        String data = new Gson().toJson(obj);
        channel.writeAndFlush(data + "\n");
    }

    protected void _connect() {
        retries -= 1;
        Bootstrap b = new Bootstrap();
        b.group(context.getLoopGroup());
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.option(ChannelOption.TCP_NODELAY, true);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15*1000);
        b.handler(new FlooChannelInitializer(this));
        FlooUrl flooUrl = handler.getUrl();
        ChannelFuture connect = b.connect(flooUrl.host, flooUrl.port);
        channel = connect.channel();
    }

    protected void connect() {
        if (retries <= 0) {
            Flog.warn("I give up connecting.");
            return;
        }
        if (channel == null) {
            _connect();
            return;
        }
        if (!channel.isOpen()) {
            channel = null;
            _connect();
            return;
        }
        try {
            channel.close().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    channel = null;
                    _connect();
                }
            });
        } catch (Throwable ignored) {}
    }

    public void shutdown() {
        if (channel != null) {
            try {
                channel.disconnect();
                channel.close();
            } catch (Exception e) {
                Flog.warn(e);
            }
            channel = null;
        }
        retries = -1;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Flog.log("Connected to %s", ctx.channel().remoteAddress());
        handler.on_connect();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        JsonObject obj = (JsonObject)new JsonParser().parse(msg);
        JsonElement name = obj.get("name");
        if (name == null) {
            Flog.warn("No name for receive, ignoring");
            return;
        }
        String requestName = name.getAsString();
        retries = MAX_RETRIES;
        delay = INITIAL_RECONNECT_DELAY;
        handler.on_data(requestName, obj);
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
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        Flog.log("Channel is now inactive.");
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) {
        Flog.log("Disconnected from %s", ctx.channel().remoteAddress());
        if (retries <= 0) {
            Flog.log("Giving up!");
            context.shutdown();
            return;
        }
        delay = Math.min(10000, Math.round((float) 1.5 * delay));
        Flog.log("Connection lost. Reconnecting in %sms", delay);
        context.setTimeout(delay, new Runnable() {
            @Override
            public void run() {
                Flog.log("Reconnecting to %s", ctx.channel().remoteAddress());
                connect();
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof ConnectException) {
            Flog.log("Failed to connect: " + cause.getMessage());
        }
        API.uploadCrash(handler, context, cause);
    }
}