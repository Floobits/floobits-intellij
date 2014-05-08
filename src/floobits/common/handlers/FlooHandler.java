package floobits.common.handlers;

import com.google.gson.JsonObject;
import com.intellij.openapi.roots.ProjectRootManager;
import floobits.FlooContext;
import floobits.common.*;
import floobits.common.protocol.send.FlooAuth;
import floobits.utilities.Flog;

import java.lang.reflect.Method;


public class FlooHandler extends BaseHandler {
    public FloobitsState state;
    InboundRequestHandler inbound;
    EditorManager editor;
    public EditorEventHandler editorEventHandler;

    public FlooHandler (final FlooContext context, FlooUrl flooUrl, boolean shouldUpload) {
        super(context);
        url = flooUrl;
        state = new FloobitsState(context, flooUrl);
        editor = new EditorManager(context, state);
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
        conn = new Connection(this);
        conn.start();
    }

    public void on_connect () {
        super.on_connect();
        editor.reset();
        context.statusMessage(String.format("Connecting to %s.", url.toString()), false);
        conn.write(new FlooAuth(new Settings(context), url.owner, url.workspace));
    }

    public void on_data (String name, JsonObject obj) {
        String method_name = "_on_" + name;
        Method method;
        try {
            method = this.getClass().getDeclaredMethod(method_name, new Class[]{JsonObject.class});
        } catch (NoSuchMethodException e) {
            Flog.warn(String.format("Could not find %s method.\n%s", method_name, e.toString()));
            return;
        }
        Object objects[] = new Object[1];
        objects[0] = obj;
        Flog.debug("Calling %s", method_name);
        try {
            method.invoke(this, objects);
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
        editor.shutdown();
        context.chatManager.clearUsers();
        state.shutdown();
    }
}
