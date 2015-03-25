package floobits.common.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import floobits.common.API;
import floobits.common.Constants;
import floobits.common.FlooUrl;
import floobits.common.Utils;
import floobits.common.interfaces.IContext;
import floobits.common.protocol.handlers.BaseHandler;
import floobits.utilities.Flog;
import io.fletty.bootstrap.Bootstrap;
import io.fletty.channel.*;
import io.fletty.channel.socket.SocketChannel;
import io.fletty.channel.socket.nio.NioSocketChannel;
import io.fletty.handler.codec.LineBasedFrameDecoder;
import io.fletty.handler.codec.string.StringDecoder;
import io.fletty.handler.codec.string.StringEncoder;
import io.fletty.handler.ssl.SslHandler;
import io.fletty.util.CharsetUtil;
import io.fletty.handler.codec.TooLongFrameException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.Serializable;
import java.net.ConnectException;
import java.util.concurrent.RejectedExecutionException;

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
    private final IContext context;
    protected Channel channel;
    private int MAX_RETRIES = 22;
    private int INITIAL_RECONNECT_DELAY = 500;
    protected volatile Integer retries = MAX_RETRIES;
    protected Integer delay = INITIAL_RECONNECT_DELAY;

    public Connection(final BaseHandler handler){
        this.handler = handler;
        this.context = handler.context;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }
    public void start() {
        connect();
    }

    public void write(Serializable obj) {
        // TODO: threading issue. lock channel
        if (channel == null) {
            Flog.warn("not writing because no channel");
            return;
        }
        String data = new Gson().toJson(obj);
        if (channel == null) {
            Flog.warn("Lost connection? Not writing because no channel. Also, race condition!");
            return;
        }
        channel.writeAndFlush(data + "\n");
    }

    protected void _connect(String host, int port) {
        Bootstrap b = new Bootstrap();

        if (!context.addGroup(b)) {
            Flog.warn("no loopgroup, will not reconnect");
            return;
        }
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.option(ChannelOption.TCP_NODELAY, true);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15*1000);
        b.handler(new FlooChannelInitializer(this));

        try {
            ChannelFuture connect = b.connect(host, port);
            channel = connect.channel();
        }   catch (RejectedExecutionException e) {
            context.errorMessage("Can not connect to floobits!");
            context.shutdown();
        }   catch (Throwable e) {
            Flog.warn(e);
            reconnect();
        }
    }

    protected void connect() {
        if (retries <= 0) {
            Flog.warn("I give up connecting.");
            return;
        }
        retries -= 1;
        FlooUrl flooUrl = handler.getUrl();
        final String host;
        final int port;

        if (flooUrl.host.equals(Constants.floobitsDomain) && retries % 4 == 0) {
            host = Constants.OUTBOUND_FILTER_PROXY_HOST;
            port = Constants.OUTBOUND_FILTER_PROXY_PORT;
        } else {
            host = flooUrl.host;
            port = flooUrl.port;
        }

        if (channel == null) {
            _connect(host, port);
            return;
        }
        try {
            channel.close().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    channel = null;
                    _connect(host, port);
                }
            });
        } catch (Throwable e) {
            Flog.warn(e);
            reconnect();
        }
    }

    protected void reconnect() {
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
                Flog.log("Attempting to reconnect.");
                connect();
            }
        });
    }

    public void shutdown() {
        retries = -1;
        if (channel != null) {
            try {
                channel.disconnect();
                channel.close();
            } catch (Throwable e) {
                Flog.warn(e);
            }
            channel = null;
        }
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
        Flog.log("%s", evt.toString());
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        Flog.log("Channel is now inactive.");
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) {
        Flog.log("Disconnected from %s", ctx.channel().remoteAddress());
        reconnect();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (retries < 0) {
            return;
        }
        if (cause instanceof ConnectException) {
            Flog.warn("Failed to connect: " + cause.getMessage());
            return;
        }
        if (cause instanceof IOException){
            Flog.warn(cause);
            return;
        }
        if (cause instanceof TooLongFrameException) {
            Flog.warn(String.format("Took too long: %s", cause.getMessage()));
            return;
        }
        if (cause instanceof SSLHandshakeException) {
            Flog.warn(String.format("SSL Handshake failed: %s", cause.getMessage()));
            return;
        }
        API.uploadCrash(handler, context, cause);
    }
}
