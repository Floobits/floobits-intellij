package floobits;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.common.*;
import floobits.dialogs.DialogBuilder;
import floobits.dialogs.SelectOwner;
import floobits.handlers.BaseHandler;
import floobits.handlers.CreateAccountHandler;
import floobits.handlers.FlooHandler;
import floobits.handlers.LinkEditorHandler;
import floobits.utilities.Flog;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

/**
 * I am the link between a project and floobits
 */
public class FlooContext {
    public String colabDir;
    public Project project;
    public BaseHandler handler;
    protected Ignore ignoreTree;
    private Timeouts timeouts;

    public FlooContext(Project project) {
        this.project = project;
    }

    public Timeout setTimeout(int time, final Runnable runnable) {
        if (timeouts == null) {
            return null;
        }
        Timeout timeout = new Timeout(time, runnable);
        timeouts.setTimeout(timeout);
        return timeout;
    }

    public void shareProject() {
        final String project_path = project.getBasePath();

        FlooUrl flooUrl = DotFloo.read(project_path);
        if (API.workspaceExists(flooUrl, this)) {
            joinWorkspace(flooUrl, project_path, true);
            return;
        }

        PersistentJson persistentJson = PersistentJson.getInstance();
        for (Map.Entry<String, Map<String, Workspace>> i : persistentJson.workspaces.entrySet()) {
            Map<String, Workspace> workspaces = i.getValue();
            for (Map.Entry<String, Workspace> j : workspaces.entrySet()) {
                Workspace w = j.getValue();
                if (Utils.isSamePath(w.path, project_path)) {
                    try {
                        flooUrl = new FlooUrl(w.url);
                    } catch (MalformedURLException e) {
                        Flog.warn(e);
                        continue;
                    }
                    if (API.workspaceExists(flooUrl, this)) {
                        joinWorkspace(flooUrl, w.path, true);
                        return;
                    }
                }
            }
        }

        Settings settings = new Settings(this);
        String owner = settings.get("username");
        final String name = new File(project_path).getName();

        final FlooContext context = this;
        RunLater<String> runLater = new RunLater<String>() {
            @Override
            public void run(String owner) {
                if (API.createWorkspace(context, owner, name)) {
                    joinWorkspace(new FlooUrl(Constants.defaultHost, owner, name, Constants.defaultPort, true), project_path, true);
                }
            }
        };

        List<String> orgs = API.getOrgsCanAdmin(this);
        if (orgs.size() == 0) {
            runLater.run(owner);
            return;
        }
        SelectOwner.build(orgs, runLater);

    }

    public void joinWorkspace(final FlooUrl flooUrl, final String path, final boolean upload) {
        if (!isJoined()) {
            setColabDir(Utils.unFuckPath(path));
            timeouts = Timeouts.create();
            handler = new FlooHandler(this, flooUrl, upload);
            handler.go();
            return;
        }

        String title = String.format("Really leave %s?", handler.url.workspace);
        String body = String.format("Leave %s and join %s ?", handler.url.toString(), handler.url.toString());
        DialogBuilder.build(title, body, new RunLater<Boolean>() {
            public void run(Boolean join) {
                if (!join) {
                    return;
                }
                shutdown();
                joinWorkspace(flooUrl, path, upload);
            }
        });
    }

    public void createAccount() {
        if (!isJoined()) {
            CreateAccountHandler createAccountHandler = new CreateAccountHandler(this);
            handler = createAccountHandler;
            createAccountHandler.go();
            return;
        }
        status_message("You already have an account and are connected with it.");
        shutdown();
    }


    public void linkEditor() {
        if (!isJoined()) {
            LinkEditorHandler linkEditorHandler = new LinkEditorHandler(this);
            handler = linkEditorHandler;
            linkEditorHandler.go();
            return;
        }
        Utils.status_message("You already have an account and are connected with it.", project);
        shutdown();
    }

    public boolean isJoined() {
        return handler != null;
    }

    public @Nullable FlooHandler getFlooHandler(){
        if (handler != null && handler instanceof FlooHandler)
            return (FlooHandler)handler;
        return null;
    }

    public void setColabDir(String colabDir) {
        this.colabDir = colabDir;
        Ignore.writeDefaultIgnores(this);
        VirtualFile fileByIoFile = VfsUtil.findFileByIoFile(new File(colabDir), true);
        ignoreTree = new Ignore(fileByIoFile);
        ignoreTree.recurse();
    }

    public String absPath(String path) {
        return Utils.absPath(colabDir, path);
    }

    public Boolean isShared(String path) {
        return Utils.isShared(path, colabDir);
    }

    public String toProjectRelPath(String path) {
        return Utils.toProjectRelPath(path, colabDir);
    }

    public Boolean isIgnored(VirtualFile f) {
        return ignoreTree.isIgnored(this, f);
    }

    public void flash_message(final String message) {
        Utils.flash_message(message, project);
    }

    public void status_message(String message, NotificationType notificationType) {
        Utils.status_message(message, notificationType, project);
    }

    public void status_message(String message) {
        Flog.log(message);
        status_message(message, NotificationType.INFORMATION);
    }

    public void error_message(String message) {
        Flog.log(message);
        status_message(message, NotificationType.ERROR);
    }

    public void shutdown() {
        if (timeouts != null) {
            timeouts.shutdown();
            timeouts = null;
        }

        if (handler != null) {
            handler.shutdown();
            handler = null;
        }
        ignoreTree = null;
    }
}
