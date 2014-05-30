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
import floobits.dialogs.SelectHost;
import floobits.dialogs.ShareProjectDialog;
import floobits.utilities.Flog;
import floobits.windows.ChatManager;
import io.fletty.channel.nio.NioEventLoopGroup;
import io.fletty.util.concurrent.ScheduledFuture;
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
    public final EditorManager editor;
    public Date lastChatMessage;
    protected NioEventLoopGroup loopGroup;

    public FlooContext(Project project) {
        this.project = project;
        editor = new EditorManager(this);
    }

    public NioEventLoopGroup getLoopGroup() {
        return loopGroup;
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
        FloorcJson floorcJson = Settings.get();
        String host = null;
        int size = floorcJson.auth.size();
        if (size <= 0) {
            return;
        } else if (size == 1) {
            host = (String)floorcJson.auth.keySet().toArray()[0];
        } else {
            SelectHost selectHost = new SelectHost(project, floorcJson.auth.keySet());
            selectHost.show();
        }


        String owner = floorcJson.auth.get(host).get("username");
        final String name = new File(project_path).getName();
        final FlooContext context = this;

        List<String> orgs = API.getOrgsCanAdmin(host, this);
        orgs.add(0, owner);
        final String finalHost = host;
        ShareProjectDialog shareProjectDialog = new ShareProjectDialog(name, orgs, project, new RunLater<ShareProjectDialog>() {
            @Override
            public void run(ShareProjectDialog dialog) {
                if (API.createWorkspace(finalHost, dialog.getOrgName(), dialog.getWorkspaceName(), context, _private_)) {
                    joinWorkspace(new FlooUrl(finalHost, dialog.getOrgName(), dialog.getWorkspaceName(), Constants.defaultPort, true), project_path, true);
                }
            }
        });
        shareProjectDialog.createCenterPanel();
        shareProjectDialog.show();

    }

    public void joinWorkspace(final FlooUrl flooUrl, final String path, final boolean upload) {
        if (setupHandler(new FlooHandler(this, flooUrl, upload, path))) {
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
        if (setupHandler(new CreateAccountHandler(this))) {
            return;
        }
        statusMessage("You already have an account and are connected with it.", false);
        shutdown();
    }


    public void linkEditor() {
        if (setupHandler(new LinkEditorHandler(this))) {
            return;
        }
        Utils.statusMessage("You already have an account and are connected with it.", project);
        shutdown();
    }

    private boolean setupHandler(BaseHandler handler) {
        if (isJoined()) {
            return false;
        }
        this.handler = handler;
        loopGroup = new NioEventLoopGroup();
        handler.go();
        return true;
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
        if (chatManager == null || !chatManager.isOpen()) {
            statusMessage(message, NotificationType.INFORMATION);
        }
        if (isChat || chatManager == null) {
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
        if (handler != null) {
            handler.shutdown();
            editor.shutdown();
            if (chatManager != null) {
                chatManager.statusMessage("Disconnecting.");
            }
            handler = null;
        }

        if (chatManager != null) {
            chatManager.clearUsers();
        }

        if (loopGroup != null) {
            try {
                loopGroup.shutdownGracefully(0, 500, TimeUnit.MILLISECONDS);
                loopGroup.awaitTermination(500, TimeUnit.MILLISECONDS);
            } catch (Throwable e) {
                Flog.warn(e);
            }
            loopGroup = null;
        }
        ignoreTree = null;
    }
}
