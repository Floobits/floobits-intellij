package floobits;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.common.*;
import floobits.common.handlers.BaseHandler;
import floobits.common.handlers.CreateAccountHandler;
import floobits.common.handlers.FlooHandler;
import floobits.common.handlers.LinkEditorHandler;
import floobits.dialogs.DialogBuilder;
import floobits.dialogs.ShareProjectDialog;
import floobits.utilities.Flog;
import floobits.windows.ChatManager;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.ScheduledFuture;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * I am the link between a project and floobits
 */
public class FlooContext {

    public String colabDir;
    public Project project;
    public BaseHandler handler;
    public ChatManager chatManager;
    protected Ignore ignoreTree;
    public Date lastChatMessage;
    protected NioEventLoopGroup loopGroup = new NioEventLoopGroup();

    public NioEventLoopGroup getLoopGroup() {
        return loopGroup;
    }

    public FlooContext(Project project) {
        this.project = project;
    }

    public ScheduledFuture setTimeout(int time, final Runnable runnable) {
        return loopGroup.schedule(runnable, time, TimeUnit.MILLISECONDS);
    }

    public boolean openFile(File file) {
        VirtualFile floorc = LocalFileSystem.getInstance().findFileByIoFile(file);
        if (floorc == null) {
            return false;
        }
        FileEditorManager.getInstance(this.project).openFile(floorc, true);
        return true;
    }

    public void loadChatManager() {
        chatManager = new ChatManager(this);
    }

    private boolean changePerms(FlooUrl flooUrl, String[] newPerms) {
        HTTPWorkspaceRequest workspace = API.getWorkspace(flooUrl, this);
        if (workspace == null) {
            return false;
        }

        String[] anonPerms = workspace.perms.get("AnonymousUser");
        if (anonPerms == null) {
            anonPerms = new String[]{};
        }
        Arrays.sort(anonPerms);
        Arrays.sort(newPerms);
        if (!Arrays.equals(anonPerms, newPerms)) {
            workspace.perms.put("AnonymousUser", newPerms);
            return API.updateWorkspace(flooUrl, workspace, this);
        }
        return true;
    }

    public void shareProject(final boolean _private_) {
        final String project_path = project.getBasePath();

        FlooUrl flooUrl = DotFloo.read(project_path);

        String[] newPerms = _private_ ? new String[]{} : new String[]{"view_room"};

        if (flooUrl != null && changePerms(flooUrl, newPerms)) {
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
                    if (changePerms(flooUrl, newPerms)) {
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

        List<String> orgs = API.getOrgsCanAdmin(this);
        orgs.add(0, owner);
        ShareProjectDialog shareProjectDialog = new ShareProjectDialog(name, orgs, project, new RunLater<ShareProjectDialog>() {
            @Override
            public void run(ShareProjectDialog dialog) {
                if (API.createWorkspace(dialog.getOrgName(), dialog.getWorkspaceName(), context, _private_)) {
                    joinWorkspace(new FlooUrl(Constants.defaultHost, dialog.getOrgName(), dialog.getWorkspaceName(), Constants.defaultPort, true), project_path, true);
                }
            }
        });
        shareProjectDialog.createCenterPanel();
        shareProjectDialog.show();

    }

    public void joinWorkspace(final FlooUrl flooUrl, final String path, final boolean upload) {
        if (!isJoined()) {
            if (!API.workspaceExists(flooUrl, this)) {
                errorMessage(String.format("The workspace %s does not exist.", flooUrl));
                return;
            }
            setColabDir(Utils.unFuckPath(path));
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
        statusMessage("You already have an account and are connected with it.", false);
        shutdown();
    }


    public void linkEditor() {
        if (!isJoined()) {
            LinkEditorHandler linkEditorHandler = new LinkEditorHandler(this);
            handler = linkEditorHandler;
            linkEditorHandler.go();
            return;
        }
        Utils.statusMessage("You already have an account and are connected with it.", project);
        shutdown();
    }

    public boolean isJoined() {
        return handler != null && handler.isJoined;
    }

    public void disconnected() {
        if (handler != null) {
            handler.isJoined = false;
        }
    }

    public @Nullable FlooHandler getFlooHandler(){
        if (handler != null && handler instanceof FlooHandler)
            return (FlooHandler)handler;
        return null;
    }

    public void setColabDir(String colabDir) {
        this.colabDir = colabDir;
        Ignore.writeDefaultIgnores(this);
        refreshIgnores();
    }

    public void refreshIgnores() {
        VirtualFile fileByIoFile = VfsUtil.findFileByIoFile(new File(colabDir), true);
        ignoreTree = Ignore.BuildIgnore(fileByIoFile);
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

    public Ignore getIgnoreTree() {
        return ignoreTree;
    }

    public void flashMessage(final String message) {
        Utils.flashMessage(message, project);
    }

    public void statusMessage(String message, NotificationType notificationType) {
        Utils.statusMessage(message, notificationType, project);
    }

    public void statusMessage(String message, boolean isChat) {
        Flog.log(message);
        if (!chatManager.isOpen()) {
            statusMessage(message, NotificationType.INFORMATION);
        }
        if (isChat) {
            // No point in setting a status message to chat for chat since it already has the chat message.
            return;
        }
        chatManager.statusMessage(message);
    }

    public void errorMessage(String message) {
        Flog.warn(message);
        statusMessage(message, NotificationType.ERROR);
        chatManager.errorMessage(message);
    }

    public void shutdown() {
        if (chatManager != null) {
            chatManager.statusMessage("Disconnecting.");
        }
        if (handler != null) {
            handler.shutdown();
            handler = null;
        }

        if (loopGroup != null) {
            try {
                loopGroup.shutdownGracefully(0, 500, TimeUnit.MILLISECONDS);
                loopGroup.awaitTermination(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Flog.warn(e);
            }
            loopGroup = new NioEventLoopGroup();
        }
        ignoreTree = null;
    }
}
