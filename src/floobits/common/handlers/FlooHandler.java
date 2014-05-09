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
    EditorManager editor;
    public EditorEventHandler editorEventHandler;

    public FlooHandler (final FlooContext context, FlooUrl flooUrl, boolean shouldUpload) {
        super(context);
        url = flooUrl;
        state = new FloobitsState(context, flooUrl);
        editor = new EditorManager(context);
        conn = new Connection(this);
        outbound = new OutboundRequestHandler(context, state, conn);
        inbound = new InboundRequestHandler(context, state, editor, outbound, shouldUpload);
        editorEventHandler = new EditorEventHandler(context, state, editor, outbound, inbound);
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
        editor.reset();
        context.statusMessage(String.format("Connecting to %s.", url.toString()), false);
        conn.write(new FlooAuth(new Settings(context), url.owner, url.workspace));
    }

    public void on_data (String name, JsonObject obj) {
        inbound.on_data(name, obj);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        context.statusMessage(String.format("Leaving workspace: %s.", url.toString()), false);
        editorEventHandler.shutdown();
        editor.shutdown();
        context.chatManager.clearUsers();
        state.shutdown();
    }
}
