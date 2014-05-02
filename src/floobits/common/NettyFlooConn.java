package floobits.common;

import com.google.gson.Gson;
import floobits.common.handlers.BaseHandler;
import floobits.utilities.Flog;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.Serializable;

public class NettyFlooConn {
    private final BaseHandler handler;
    private EventLoopGroup workerGroup;
    public int RECONNECT_DELAY = 15;
    ChannelFuture connect;
    Channel channel;

    public NettyFlooConn(final BaseHandler handler, EventLoopGroup workerGroup){
        this.handler = handler;
        this.workerGroup = workerGroup;
    }

    public Bootstrap bootstrap() {
        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.option(ChannelOption.TCP_NODELAY, true);
        final NettyFlooConn self = this;
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                SSLContext sslContext = Utils.createSSLContext();
                SSLEngine engine = sslContext.createSSLEngine();
                engine.setUseClientMode(true);
                pipeline.addLast("ssl", new SslHandler(engine));
                // On top of the SSL handler, add the text line codec.
                pipeline.addLast("framer", new DelimiterBasedFrameDecoder(1000 * 1000 * 10, true, Delimiters.lineDelimiter()));
                pipeline.addLast("decoder", new StringDecoder());
                pipeline.addLast("encoder", new StringEncoder());
                // and then business logic.
                pipeline.addLast("handler", handler);
            }
        });
        return b;
    }
    public void connect() {
        try {
            Bootstrap bootstrap = bootstrap();
            FlooUrl flooUrl = handler.getUrl();
            connect = bootstrap.connect(flooUrl.host, flooUrl.port);
            ChannelFuture channelFuture = connect.sync();
            channel = channelFuture.channel();
            // Wait until the connection is closed.
        } catch (InterruptedException e) {
            Flog.warn(e);
        }
    }

    public void write(Serializable obj) {
       String data = new Gson().toJson(obj);
       channel.write(data + "\n");
    }

    public void shutdown() {
        try {
            channel.closeFuture().sync();
        } catch (Exception e) {
            Flog.warn(e);
        }
        connect.cancel(true);
    }

    public void start()  {
        connect();
    }
}

