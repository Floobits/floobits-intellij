package floobits;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.common.*;
import floobits.common.handlers.BaseHandler;
import floobits.common.handlers.FlooHandler;
import floobits.windows.ChatManager;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.Date;

public abstract class BaseContext {
    public String colabDir;
    public Project project;
    public BaseHandler handler;
    public ChatManager chatManager;
    public Date lastChatMessage;
    protected Ignore ignoreTree;
    protected Timeouts timeouts;

    public BaseContext(Project project) {
        this.project = project;
    }

    public boolean isJoined() {
        return handler != null;
    }

    public Timeout setTimeout(int time, final Runnable runnable) {
        if (timeouts == null) {
            return null;
        }
        Timeout timeout = new Timeout(time, runnable);
        timeouts.setTimeout(timeout);
        return timeout;
    }

    protected boolean changePerms(FlooUrl flooUrl, String[] newPerms) {
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

    public @Nullable
    abstract FlooHandler getFlooHandler();

    public void setColabDir(String colabDir) {
        this.colabDir = colabDir;
        Ignore.writeDefaultIgnores(this);
        refreshIgnores();
    }

    public void refreshIgnores() {
        VirtualFile fileByIoFile = VfsUtil.findFileByIoFile(new File(colabDir), true);
        ignoreTree = Ignore.BuildIgnore(fileByIoFile);
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

    public abstract void loadChatManager();

    public abstract void createAccount();

    public abstract void linkEditor();

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

    public abstract void flashMessage(String message);

    public abstract void statusMessage(String message, NotificationType notificationType);

    public abstract void statusMessage(String message, boolean isChat);

    public abstract void errorMessage(String message);

    public abstract boolean openFile(File file);

    public abstract void shareProject(boolean _private_);

    public abstract void joinWorkspace(FlooUrl flooUrl, String path, boolean upload);

}
