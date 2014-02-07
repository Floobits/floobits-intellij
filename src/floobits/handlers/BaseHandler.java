package floobits.handlers;

import com.google.gson.JsonObject;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import floobits.FlooContext;
import floobits.FloobitsPlugin;
import floobits.common.FlooConn;
import floobits.common.FlooUrl;
import floobits.utilities.Flog;

import java.util.ArrayList;

abstract public class BaseHandler {
    public FlooUrl url;
    public boolean disconnected = false;
    public boolean isJoined = false;
    protected FlooConn conn;
    public FlooContext context;

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

    public abstract void go();

    public void shutDown() {
        if (this.conn != null && this.conn.shutDown()) {
            this.conn = null;
        }
        disconnected = true;
        isJoined = false;
        context.removeHandler();
    }
}
