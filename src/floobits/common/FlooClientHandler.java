package floobits.common;

import floobits.common.handlers.BaseHandler;

/**
 * Created by kans on 5/1/14.
 */

public class FlooClientHandler {
    private long startTime = -1;
    private BaseHandler handler;
    private Connection connection;

    public FlooClientHandler(BaseHandler handler, Connection connection) {
        this.handler = handler;
        this.connection = connection;
    }

}