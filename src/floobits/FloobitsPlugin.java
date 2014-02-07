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
    public final static String name = "Floobits-Plugin";
    public final FlooContext context = new FlooContext();

    public static FloobitsPlugin getInstance(Project project) {
        return project.getComponent(FloobitsPlugin.class);
    }
    public FloobitsPlugin() {
        Flog.info("Floobits plugin");
    }

    public void shareProject(final Project project) {
        if (!context.isJoined()) {
            context.project = project;
            context.handler = new FlooHandler(context, project);
            if (context.handler.disconnected) {
                context.removeHandler();
            }
            return;
        }

        String title = String.format("Really leave %s?", context.handler.url.workspace);
        String body = String.format("You are currently in the workspace: %s.  Do you want to join %s?", context.handler.url.toString(), context.handler.url.toString());
        DialogBuilder.build(title, body, new RunLater<Boolean>() {

            public void run(Boolean join) {
                if (!join) {
                    return;
                }
                context.handler.shutDown();
                shareProject(project);
            }
        });
    }

    public void joinWorkspace(final Project project, final String url) {
        if (!context.isJoined()) {
            FlooUrl f;
            try {
                f = new FlooUrl(url);
            } catch (Exception e) {
                Flog.warn(e);
                return;
            }
            context.project = project;
            context.handler = new FlooHandler(context, f);
            if (context.handler.disconnected) {
                context.removeHandler();
            }
            return;
        }
        String title = String.format("Really leave %s?", context.handler.url.workspace);
        String body = String.format("You are currently in the workspace: %s.  Do you want to join %s?", context.handler.url.toString(), context.handler.url.toString());
        DialogBuilder.build(title, body, new RunLater<Boolean>() {

            public void run(Boolean join) {
                if (!join) {
                    return;
                }
                context.handler.shutDown();
                joinWorkspace(project, url);
            }
        });
    }

    public void createAccount(Project project) {
        if (!context.isJoined()) {
            CreateAccountHandler createAccountHandler = new CreateAccountHandler(context);
            createAccountHandler.create();
            return;
        }
        context.status_message("You already have an account and are connected with it.");
        context.handler.shutDown();
    }


    public void linkEditor(Project project) {
        if (!context.isJoined()) {
            context.handler = new LinkEditorHandler(context);
            ((LinkEditorHandler)context.handler).link();
            return;
        }
        Utils.status_message("You already have an account and are connected with it.", project);
        context.handler.shutDown();
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
}
