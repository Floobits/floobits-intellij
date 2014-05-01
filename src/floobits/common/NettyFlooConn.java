package floobits.common;

import floobits.common.handlers.BaseHandler;
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

public class NettyFlooConn {
    private final BaseHandler handler;
    private EventLoopGroup workerGroup;
    public int RECONNECT_DELAY = 15;

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
                pipeline.addLast("handler", new FlooClientHandler(handler, self));
            }
        });
        return b;
    }
    public void connect() {
        try {
            Bootstrap bootstrap = bootstrap();
            FlooUrl flooUrl = handler.getUrl();
            ChannelFuture f = bootstrap.connect(flooUrl.host, flooUrl.port).sync();
            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public void start()  {
        connect();
    }
}

