package floobits.common;

import com.google.gson.JsonObject;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;
import floobits.utilities.Flog;

import java.util.ArrayList;

abstract public class BaseHandler {
    public FlooUrl url;
    public boolean disconnected = false;
    public boolean isJoined = false;
    protected FlooConn conn;
    protected FloobitsPlugin context;

    public BaseHandler(FloobitsPlugin context) {
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

    public void shutDown() {
        if (this.conn != null && this.conn.shutDown()) {
            this.conn = null;
            status_message(String.format("Leaving workspace: %s.", url.toString()));
        }
        disconnected = true;
        isJoined = false;
        context.removeHandler();
    }

    public void flash_message(final String message) {
        Utils.flash_message(message, context.project);
    }

    public void status_message(String message, NotificationType notificationType) {
        Utils.status_message(message, notificationType, context.project);
    }

    public void status_message(String message) {
        Flog.log(message);
        status_message(message, NotificationType.INFORMATION);
    }

    public void error_message(String message) {
        Flog.log(message);
        status_message(message, NotificationType.ERROR);
    }
}
