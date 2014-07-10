package floobits.common.protocol.handlers;

import com.google.gson.JsonObject;
import floobits.common.*;
import floobits.common.interfaces.IContext;
import floobits.common.protocol.Connection;
import floobits.common.protocol.json.send.FlooAuth;
import floobits.utilities.Flog;

import java.util.HashMap;


public class FlooHandler extends BaseHandler {
    private final HashMap<String, String> auth;
    private final boolean shouldUpload;
    public FloobitsState state;
    InboundRequestHandler inbound;
    public EditorEventHandler editorEventHandler;

    public FlooHandler(final IContext context, FlooUrl flooUrl, boolean shouldUpload, String path, HashMap<String, String> auth) {
        super(context);
        this.auth = auth;
        this.shouldUpload = shouldUpload;
        context.setColabDir(Utils.unFuckPath(path));
        url = flooUrl;
        state = new FloobitsState(context, flooUrl);
        state.username = auth.get("username");
    }

    public void go() {
        super.go();
        Flog.log("joining workspace %s", url);
        conn = new Connection(this);
        outbound = new OutboundRequestHandler(context, state, conn);
        inbound = new InboundRequestHandler(context, state, outbound, shouldUpload);
        editorEventHandler = new EditorEventHandler(context, state, outbound, inbound);
//        if (ProjectRootManager.getInstance(context.project).getProjectSdk() == null) {
//            Flog.warn("No SDK detected.");
//        }
        PersistentJson persistentJson = PersistentJson.getInstance();
        persistentJson.addWorkspace(url, context.colabDir);
        persistentJson.save();
        conn.start();
        editorEventHandler.go();
    }

    public void on_connect () {
        context.editor.reset();
        context.statusMessage(String.format("Connecting to %s.", url.toString()));
        conn.write(new FlooAuth(auth.get("username"), auth.get("api_key"), auth.get("secret"), url.owner, url.workspace));
    }

    public void _on_data (String name, JsonObject obj) {
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
        context.statusMessage(String.format("Leaving workspace: %s.", url.toString()));
        state.shutdown();
    }
}
