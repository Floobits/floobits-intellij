package floobits.common.handlers;

import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import floobits.FlooContext;
import floobits.common.Connection;
import floobits.common.FlooUrl;
import floobits.common.OutboundRequestHandler;

abstract public class BaseHandler {
    public FlooUrl url;
    public boolean isJoined = false;
    protected Connection conn;
    public FlooContext context;
    public OutboundRequestHandler outbound;

    public BaseHandler(FlooContext context) {
        this.context = context;
    }

    public abstract void on_data(String name, JsonObject obj);
    public abstract void on_connect();

    public Project getProject() {
        return context.project;
    }

    public FlooUrl getUrl() {
        return url;
    }

    public void go() {
        isJoined = true;
    }

    public void shutdown() {
        if (conn != null) {
            conn.shutdown();
            conn = null;
        }
        isJoined = false;
    }
}
