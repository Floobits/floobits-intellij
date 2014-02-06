package floobits;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.common.*;
import floobits.dialogs.DialogBuilder;
import floobits.handlers.CreateAccountHandler;
import floobits.handlers.FlooHandler;
import floobits.handlers.LinkEditorHandler;
import floobits.utilities.Flog;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Created by kans on 2/6/14.
 */
public class FlooContext {
    public String colabDir;
    public Project project;
    public BaseHandler handler;
    protected Ignore ignoreTree;

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

    public void setColabDir(String colabDir) {
        this.colabDir = colabDir;
        try {
            ignoreTree = new Ignore(new File(colabDir), null, 0);
        } catch (IOException e) {
            error_message("Your file system may be read-only or you may not have access to it.");
            return;
        }
        LinkedList<Ignore> queue = new LinkedList<Ignore>();
        queue.add(ignoreTree);
        while (queue.size() > 0) {
            Ignore current = queue.pop();
            File[] childDirectories = current.file.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            });
            if (childDirectories != null){
                for (File childDirectory : childDirectories) {
                    Ignore child = current.adopt(childDirectory);
                    if (child == null) {
                        continue;
                    }
                    queue.push(child);
                }
            }
        }
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

    public boolean isIgnored(final String path) {
        return ignoreTree != null && ignoreTree.isIgnored(this, path);
    }
    public Boolean isIgnored(VirtualFile f) {
        return f.isValid() && isIgnored(f.getPath());
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
}
