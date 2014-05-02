package floobits.common;

import floobits.common.handlers.BaseHandler;

/**
 * Created by kans on 5/1/14.
 */

public class FlooClientHandler {
    private long startTime = -1;
    private BaseHandler handler;
    private NettyFlooConn nettyFlooConn;

    public FlooClientHandler(BaseHandler handler, NettyFlooConn nettyFlooConn) {
        this.handler = handler;
        this.nettyFlooConn = nettyFlooConn;
    }

}