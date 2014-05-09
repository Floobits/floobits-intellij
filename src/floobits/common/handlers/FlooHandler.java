package floobits.common.handlers;

import com.google.gson.JsonObject;
import com.intellij.openapi.roots.ProjectRootManager;
import floobits.FlooContext;
import floobits.common.*;
import floobits.common.protocol.send.FlooAuth;
import floobits.utilities.Flog;


public class FlooHandler extends BaseHandler {
    public FloobitsState state;
    InboundRequestHandler inbound;
    public EditorEventHandler editorEventHandler;

    public FlooHandler (final FlooContext context, FlooUrl flooUrl, boolean shouldUpload) {
        super(context);
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
        conn.write(new FlooAuth(new Settings(context), url.owner, url.workspace));
    }

    public void on_data (String name, JsonObject obj) {
        Flog.debug("Calling %s", name);
        try {
            inbound.on_data(name, obj);
        } catch (Exception e) {
            Flog.warn(String.format("on_data error \n\n%s", Utils.stackToString(e)));
            if (name.equals("room_info")) {
                context.errorMessage("There was a critical error in the plugin" + e.toString());
                context.shutdown();
            }
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
