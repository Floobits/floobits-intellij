package floobits;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;

import floobits.impl.IntellijVFactory;
import floobits.common.*;
import floobits.common.handlers.BaseHandler;
import floobits.common.handlers.CreateAccountHandler;
import floobits.common.handlers.FlooHandler;
import floobits.common.handlers.LinkEditorHandler;
import floobits.common.interfaces.VFactory;
import floobits.common.interfaces.VFile;
import floobits.dialogs.DialogBuilder;
import floobits.dialogs.SelectAccount;
import floobits.dialogs.ShareProjectDialog;
import floobits.utilities.Flog;
import floobits.windows.ChatManager;
import io.fletty.bootstrap.Bootstrap;
import io.fletty.channel.nio.NioEventLoopGroup;
import io.fletty.util.concurrent.ScheduledFuture;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * I am the link between a project and floobits
 */
public class FlooContext {

    public String colabDir;
    public Project project;
    public volatile BaseHandler handler;
    public ChatManager chatManager;
    protected Ignore ignoreTree;
    public final EditorScheduler editor;
    public Date lastChatMessage;
    public VFactory vFactory;
    protected volatile NioEventLoopGroup loopGroup;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public FlooContext(Project project) {
        this.project = project;
        editor = new EditorScheduler(this);
        this.vFactory = new IntellijVFactory(this, editor);
    }

    public boolean addGroup(Bootstrap b) {
        boolean b1 = false;
        try {
            lock.readLock().lock();
            if (loopGroup != null) {
                b.group(loopGroup);
                b1 = true;
            }
        } finally {
            lock.readLock().unlock();
        }
        return b1;
    }

    public ScheduledFuture setTimeout(int time, final Runnable runnable) {
        ScheduledFuture schedule = null;
        try {
            lock.readLock().lock();
            if (loopGroup != null) {
                schedule = loopGroup.schedule(runnable, time, TimeUnit.MILLISECONDS);
            }
        } finally {
            lock.readLock().unlock();
        }
        return schedule;
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

        String host;
        FloorcJson floorcJson;
        try {
            floorcJson = Settings.get();
        } catch (Throwable e) {
            Flog.log("Invalid .floorc.json");
            statusMessage("Invalid .floorc.json");
            return;
        }
        int size = floorcJson.auth.size();
        if (size <= 0) {
            Flog.log("No credentials.");
            return;
        }
        String[] keys = new String[size];
        floorcJson.auth.keySet().toArray(keys);

        if (keys.length == 1) {
            host = keys[0];
        } else {
            SelectAccount selectAccount = new SelectAccount(project, keys);
            selectAccount.show();
            int exitCode = selectAccount.getExitCode();
            if (exitCode != DialogWrapper.OK_EXIT_CODE) {
                return;
            }
            host = selectAccount.getAccount();
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
        FloorcJson floorcJson = null;
        try {
            floorcJson = Settings.get();
        } catch (Throwable e) {
            statusMessage("Invalid JSON in your .floorc.json.");
        }

        HashMap<String, String> auth = floorcJson != null ? floorcJson.auth.get(flooUrl.host) : null;
        if (auth == null) {
            setupHandler(new LinkEditorHandler(this, flooUrl.host, new Runnable() {
                @Override
                public void run() {
                    joinWorkspace(flooUrl, path, upload);
                }
            }));
            return;
        }

        if (!API.workspaceExists(flooUrl, this)) {
            errorMessage(String.format("The workspace %s does not exist!", flooUrl.toString()));
            return;
        }

        if (setupHandler(new FlooHandler(this, flooUrl, upload, path, auth))) {
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
        if (setupHandler(new CreateAccountHandler(this, Constants.defaultHost))) {
            return;
        }
        statusMessage("You already have an account and are connected with it.");
        shutdown();
    }


    public void linkEditor() {
        if (setupHandler(new LinkEditorHandler(this, Constants.defaultHost))) {
            return;
        }
        Utils.statusMessage("You already have an account and are connected with it.", project);
        shutdown();
    }

    private boolean setupHandler(BaseHandler handler) {
        if (isJoined()) {
            return false;
        }

        lock.writeLock().lock();
        this.handler = handler;
        loopGroup = new NioEventLoopGroup();
        lock.writeLock().unlock();
        handler.go();
        return true;
    }

    public boolean isJoined() {
        return handler != null && handler.isJoined;
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
        VFile fileByIoFile = vFactory.findFileByIoFile(new File(colabDir));
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

    public Boolean isIgnored(VFile f) {
        String path = f.getPath();

        if (!isShared(path)) {
            Flog.log("Ignoring %s because it isn't shared.", path);
            return true;
        }

        return ignoreTree.isIgnored(f, path, toProjectRelPath(path), f.isDirectory());
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

    public void warnMessage(String message) {
        Flog.log(message);
        if (chatManager != null && chatManager.isOpen()) {
            chatManager.statusMessage(message);
        }
        statusMessage(message, NotificationType.WARNING);
    }

    public void statusMessage(String message) {
        Flog.log(message);
        if (chatManager != null && chatManager.isOpen()) {
            chatManager.statusMessage(message);
        }
        statusMessage(message, NotificationType.INFORMATION);
    }

    public void errorMessage(String message) {
        Flog.warn(message);
        statusMessage(message, NotificationType.ERROR);
        if (chatManager != null && chatManager.isOpen()) {
            chatManager.errorMessage(message);
        }
    }

    public void shutdown() {
        lock.writeLock().lock();
        try {
            if (handler != null) {
                handler.shutdown();
                editor.shutdown();
                statusMessage("Disconnecting.");
                handler = null;
            }
            if (vFactory != null) {
                vFactory.clearReadOnlyState();
            }
            if (chatManager != null) {
                chatManager.clearUsers();
            }

            if (loopGroup != null) {
                try {
                    loopGroup.shutdownGracefully(0, 500, TimeUnit.MILLISECONDS);
                } catch (Throwable e) {
                    Flog.warn(e);
                } finally {
                    loopGroup = null;
                }
            }
            ignoreTree = null;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
