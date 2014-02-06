package floobits;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import floobits.common.BaseHandler;
import floobits.common.FlooUrl;
import floobits.common.RunLater;
import floobits.common.Utils;
import floobits.dialogs.DialogBuilder;
import floobits.handlers.CreateAccountHandler;
import floobits.handlers.FlooHandler;
import floobits.handlers.LinkEditorHandler;
import floobits.utilities.Flog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class FloobitsPlugin implements ProjectComponent {
    public String colabDir;
    public Project project;
    public BaseHandler handler;
    public final static String name = "Floobits-Plugin";

    public static FloobitsPlugin getInstance(Project project) {
        return project.getComponent(FloobitsPlugin.class);
    }

    public boolean isJoined() {
        return handler != null && handler.isJoined;
    }

    public @Nullable FlooHandler getFlooHandler(){
        if (handler != null && handler instanceof FlooHandler)
            return (FlooHandler)handler;
        return null;
    }

    public void removeHandler() {
        handler = null;
    }

    public FloobitsPlugin() {
        Flog.info("Floobits plugin");
    }

    public void shareProject(final Project project) {
        if (!isJoined()) {
            this.project = project;
            handler = new FlooHandler(this, project);
            if (handler.disconnected) {
                removeHandler();
            }
            return;
        }

        String title = String.format("Really leave %s?", handler.url.workspace);
        String body = String.format("You are currently in the workspace: %s.  Do you want to join %s?", handler.url.toString(), handler.url.toString());
        DialogBuilder.build(title, body, new RunLater<Boolean>() {
            @Override
            public void run(Boolean join) {
                if (!join) {
                    return;
                }
                handler.shutDown();
                shareProject(project);
            }
        });
    }

    public void joinWorkspace(final Project project, final String url) {
        if (!isJoined()) {
            FlooUrl f;
            try {
                f = new FlooUrl(url);
            } catch (Exception e) {
                Flog.warn(e);
                return;
            }
            this.project = project;
            handler = new FlooHandler(this, f);
            if (handler.disconnected) {
                removeHandler();
            }
            return;
        }
        String title = String.format("Really leave %s?", handler.url.workspace);
        String body = String.format("You are currently in the workspace: %s.  Do you want to join %s?", handler.url.toString(), handler.url.toString());
        DialogBuilder.build(title, body, new RunLater<Boolean>() {
            @Override
            public void run(Boolean join) {
                if (!join) {
                    return;
                }
                handler.shutDown();
                joinWorkspace(project, url);
            }
        });
    }

    public void createAccount(Project project) {
        if (isJoined()) {
            FlooHandler flooHandler = getFlooHandler();
            if (flooHandler == null) {
                return;
            }
            flooHandler.status_message("You already have an account and are connected with it.");
            return;
        }
        CreateAccountHandler createAccountHandler = new CreateAccountHandler(this);
        createAccountHandler.create();
    }

    public void linkEditor(Project project) {
        if (isJoined()) {
            Utils.status_message("You already have an account and are connected with it.", project);
            handler.shutDown();
        }
        handler = new LinkEditorHandler(this);
        ((LinkEditorHandler)handler).link();
    }

    public String absPath(String path) {
        return Utils.absPath(colabDir, path);
    }

    public Boolean isShared (String path) {
        return Utils.isShared(path, colabDir);
    }

    public String toProjectRelPath (String path) {
        return Utils.toProjectRelPath(path, colabDir);
    }
    @Override
    public void projectOpened() {
    }

    @Override
    public void projectClosed() {
    }

    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {
    }

    @NotNull
    @Override
    public String getComponentName() {
        return name;
    }
    // TODO: we can store state using intellij if we want with getState and loadState
}
