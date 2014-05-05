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
import io.netty.util.ReferenceCountUtil;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.Serializable;
import java.net.ConnectException;

public class NettyFlooConn extends SimpleChannelInboundHandler<String> {
    private class FlooChannelInitializer extends ChannelInitializer<SocketChannel> {
        private NettyFlooConn nettyFlooConn;

        private FlooChannelInitializer(NettyFlooConn nettyFlooConn) {
            this.nettyFlooConn = nettyFlooConn;
        }

        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            SSLContext sslContext = Utils.createSSLContext();
            SSLEngine engine = sslContext.createSSLEngine();
            engine.setUseClientMode(true);
            pipeline.addLast("ssl", new SslHandler(engine));
            // On top of the SSL handler, add the text line codec.
            pipeline.addLast("framer", new LineBasedFrameDecoder(1000 * 1000 * 10, true, false));
            pipeline.addLast("decoder", new StringDecoder(CharsetUtil.UTF_8));
            pipeline.addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));
            // and then business logic.
            pipeline.addLast("handler", nettyFlooConn);
        }
    }
    private final BaseHandler handler;
    private final FlooContext context;
    Channel channel;
    EventLoopGroup workerGroup;
    private Integer MAX_RETRIES = 20;
    private Integer INITIAL_RECONNECT_DELAY = 500;
    protected volatile Integer retries = MAX_RETRIES;
    protected Integer delay = INITIAL_RECONNECT_DELAY;

    public NettyFlooConn(final BaseHandler handler, EventLoopGroup workerGroup){
        this.handler = handler;
        this.context = handler.context;
        this.workerGroup = workerGroup;

    }

    public void start() {
        connect();
    }

    public void write(Serializable obj) {
        String data = new Gson().toJson(obj);
        channel.write(data + "\n");
        channel.flush();
    }

    public Bootstrap bootstrap() {
        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.option(ChannelOption.TCP_NODELAY, true);
        b.handler(new FlooChannelInitializer(this));
        return b;
    }
    public void connect() {
        if (retries <= 0) {
            Flog.warn("I give up connecting.");
            return;
        }
        try {
            retries -= 1;
            Bootstrap bootstrap = bootstrap();
            FlooUrl flooUrl = handler.getUrl();
            ChannelFuture connect = bootstrap.connect(flooUrl.host, flooUrl.port).sync();
            channel = connect.channel();
            // Wait until the connection is closed.
            Flog.info("lost connection!");
        } catch (InterruptedException e) {
            Flog.warn(e);
        }
    }

    public void shutdown() {
        try {
            channel.close();
        } catch (Exception e) {
            Flog.warn(e);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Flog.log("Connected to %s", ctx.channel().remoteAddress());
        retries = MAX_RETRIES;
        delay = INITIAL_RECONNECT_DELAY;
        handler.on_connect();
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
            handler.on_data(requestName, obj);
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
    public void channelUnregistered(final ChannelHandlerContext ctx) {
        delay = Math.min(10000, Math.round((float) 1.5 * delay));
        Flog.log("Connection lost. Reconnecting in %sms", delay);
        context.setTimeout(delay, new Runnable() {
            @Override
            public void run() {
                Flog.log("Reconnecting to: " + ctx.channel().remoteAddress());
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