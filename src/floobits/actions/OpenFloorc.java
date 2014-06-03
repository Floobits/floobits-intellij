package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.FloobitsPlugin;
import floobits.common.Settings;
import floobits.utilities.Flog;

import java.io.File;
import java.io.IOException;

/**
 * Created by kans on 2/18/14.
 */
public class OpenFloorc extends AnAction {
    public void actionPerformed(AnActionEvent actionEvent) {
        Project project = actionEvent.getProject();

        FloobitsPlugin floobitsPlugin = FloobitsPlugin.getInstance(project);
        File file = new File(Settings.floorcJsonPath);
        if (!file.exists()) {
            boolean created;
            try {
                created = file.createNewFile();
            } catch (IOException e) {
                floobitsPlugin.context.errorMessage("Can not create ~/.floorc");
                Flog.warn(e);
                return;
            }
            if (!created) {
                floobitsPlugin.context.errorMessage("Can not create ~/.floorc");
                return;
            }
        }
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(Settings.floorcJsonPath);
        if (virtualFile == null) {
            floobitsPlugin.context.errorMessage("no virtual file");
            return;
        }
        if (project == null) {
            floobitsPlugin.context.errorMessage("Can not open floorc");
            return;
        }
        OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, virtualFile);
        FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true);
    }
}
