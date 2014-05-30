package floobits.common.handlers;

import com.google.gson.JsonObject;
import com.intellij.openapi.roots.ProjectRootManager;
import floobits.FlooContext;
import floobits.common.*;
import floobits.common.protocol.send.FlooAuth;
import floobits.utilities.Flog;

import java.util.HashMap;


public class FlooHandler extends BaseHandler {
    public FloobitsState state;
    InboundRequestHandler inbound;
    public EditorEventHandler editorEventHandler;

    public FlooHandler(final FlooContext context, FlooUrl flooUrl, boolean shouldUpload, String path) {
        super(context);
        if (!API.workspaceExists(flooUrl, context)) {
            context.errorMessage(String.format("The workspace %s does not exist.", flooUrl));
            return;
        }
        context.setColabDir(Utils.unFuckPath(path));
        url = flooUrl;
        state = new FloobitsState(context, flooUrl);
        conn = new Connection(this);
        outbound = new OutboundRequestHandler(context, state, conn);
        inbound = new InboundRequestHandler(context, state, outbound, shouldUpload);
        editorEventHandler = new EditorEventHandler(context, state, outbound, inbound);
        if (ProjectRootManager.getInstance(context.project).getProjectSdk() == null) {
            Flog.warn("No SDK detected.");
        }
    }

    public void go() {
        Flog.log("joining workspace %s", url);
        PersistentJson persistentJson = PersistentJson.getInstance();
        persistentJson.addWorkspace(url, context.colabDir);
        persistentJson.save();
        conn.start();
        editorEventHandler.go();
    }

    public void on_connect () {
        super.on_connect();
        context.editor.reset();
        context.statusMessage(String.format("Connecting to %s.", url.toString()), false);
        FloorcJson floorcJson = Settings.get();
        HashMap<String, String> auth = floorcJson.auth.get(url.host);
        String username = auth.get("username");
        state.username = username;
        conn.write(new FlooAuth(username, auth.get("api_key"), auth.get("secret"), url.owner, url.workspace));
    }

    public void on_data (String name, JsonObject obj) {
        Flog.debug("Calling %s", name);
        try {
            inbound.on_data(name, obj);
        } catch (Throwable e) {
            Flog.warn(String.format("on_data error \n\n%s", e.toString()));
            API.uploadCrash(this, context, e);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        context.statusMessage(String.format("Leaving workspace: %s.", url.toString()), false);
        editorEventHandler.shutdown();
        state.shutdown();
    }
}
