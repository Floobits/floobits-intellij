package floobits;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.project.Project;
import floobits.common.FlooUrl;
import floobits.common.RunLater;
import floobits.dialogs.DialogBuilder;
import org.jetbrains.annotations.NotNull;


public class FloobitsPlugin implements ApplicationComponent {
    public static FlooHandler flooHandler;

    public FloobitsPlugin() {
        Flog.info("Floobits plugin");
    }

    @Override
    public void initComponent() {
    }

    public static void shareProject(final Project project) {
        FlooHandler f = flooHandler;
        if (!FlooHandler.isJoined) {
            flooHandler = new FlooHandler(project);
            return;
        }

        String title = String.format("Really leave %s?", f.url.workspace);
        String body = String.format("You are currently in the workspace: %s.  Do you want to join %s?", f.url.toString(), f.url.toString());
        DialogBuilder.build(title, body, new RunLater<Boolean>() {
            @Override
            public void run(Boolean join) {
                if (!join) {
                    return;
                }
                flooHandler.shutDown();
                shareProject(project);
            }
        });
    }

    public static void joinWorkspace(final String url) {
        if (!FlooHandler.isJoined) {
            FlooUrl f;
            try {
                f = new FlooUrl(url);
            } catch (Exception e) {
                Flog.warn(e);
                return;
            }
            flooHandler = new FlooHandler(f);
            return;
        }
        String title = String.format("Really leave %s?", flooHandler.url.workspace);
        String body = String.format("You are currently in the workspace: %s.  Do you want to join %s?", flooHandler.url.toString(), flooHandler.url.toString());
        DialogBuilder.build(title, body, new RunLater<Boolean>() {
            @Override
            public void run(Boolean join) {
                if (!join) {
                    return;
                }
                flooHandler.shutDown();
                joinWorkspace(url);
            }
        });

    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public String toString() {
        return super.toString();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public int hashCode() {
        return super.hashCode();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void disposeComponent() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "floobits";
    }
    // TODO: we can store state using intellij if we want with getState and loadState
}
