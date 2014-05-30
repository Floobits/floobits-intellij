package floobits.common.handlers;

import com.google.gson.JsonObject;
import com.intellij.openapi.roots.ProjectRootManager;
import floobits.FlooContext;
import floobits.common.*;
import floobits.common.protocol.send.FlooAuth;
import floobits.utilities.Flog;


public class FlooHandler extends BaseHandler {
    private final boolean shouldUpload;
    public FloobitsState state;
    InboundRequestHandler inbound;
    public EditorEventHandler editorEventHandler;

    public FlooHandler(final FlooContext context, FlooUrl flooUrl, boolean shouldUpload, String path) {
        super(context);
        this.shouldUpload = shouldUpload;
        if (!API.workspaceExists(flooUrl, context)) {
            context.errorMessage(String.format("The workspace %s does not exist.", flooUrl));
            return;
        }
        context.setColabDir(Utils.unFuckPath(path));
        url = flooUrl;
    }

    public void go() {
        super.go();
        Flog.log("joining workspace %s", url);
        state = new FloobitsState(context, url);
        conn = new Connection(this);
        outbound = new OutboundRequestHandler(context, state, conn);
        inbound = new InboundRequestHandler(context, state, outbound, shouldUpload);
        editorEventHandler = new EditorEventHandler(context, state, outbound, inbound);
        if (ProjectRootManager.getInstance(context.project).getProjectSdk() == null) {
            Flog.warn("No SDK detected.");
        }
        PersistentJson persistentJson = PersistentJson.getInstance();
        persistentJson.addWorkspace(url, context.colabDir);
        persistentJson.save();
        conn.start();
        editorEventHandler.go();
    }

    public void on_connect () {
        context.editor.reset();
        context.statusMessage(String.format("Connecting to %s.", url.toString()), false);
        Settings settings = new Settings(context);
        state.username = settings.get("username");
        conn.write(new FlooAuth(settings, url.owner, url.workspace));
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
