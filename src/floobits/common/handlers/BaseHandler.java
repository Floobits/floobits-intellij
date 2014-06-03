package floobits.common.handlers;

import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import floobits.FlooContext;
import floobits.common.Connection;
import floobits.common.FlooUrl;
import floobits.common.OutboundRequestHandler;
import floobits.utilities.Flog;

abstract public class BaseHandler {
    public FlooUrl url;
    public boolean isJoined = false;
    protected Connection conn;
    public FlooContext context;
    public OutboundRequestHandler outbound;

    public BaseHandler(FlooContext context) {
        this.context = context;
    }

    void _on_error(JsonObject jsonObject) {
        String reason = jsonObject.get("msg").getAsString();
        reason = String.format("Floobits Error: %s", reason);
        Flog.warn(reason);
        if (jsonObject.has("flash") && jsonObject.get("flash").getAsBoolean()) {
            context.errorMessage(reason);
            context.flashMessage(reason);
        }
    }

    void _on_disconnect(JsonObject jsonObject) {
        String reason = jsonObject.get("reason").getAsString();
        if (reason != null) {
            context.errorMessage(String.format("You have been disconnected from the workspace because %s", reason));
            context.flashMessage("You have been disconnected from the workspace.");
        } else {
            context.statusMessage("You have left the workspace");
        }
        context.shutdown();
    }

    protected abstract void _on_data(String name, JsonObject obj);

    public void on_data(String name, JsonObject obj) {
        if (name.equals("error")) {
            _on_error(obj);
            return;
        }
        if (name.equals("disconnect")) {
            _on_disconnect(obj);
            return;
        }
        _on_data(name, obj);
    }

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
