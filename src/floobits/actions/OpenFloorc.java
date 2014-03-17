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
        File file = new File(Settings.floorcPath);
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
            Settings settings = new Settings(floobitsPlugin.context);
            settings.set("#I am emtpy:", "https://floobits.com/help/floorc/");
            settings.write();
        }
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(Settings.floorcPath);
        if (virtualFile == null) {
            floobitsPlugin.context.errorMessage("no virtual file");
            return;
        }
        OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, virtualFile);
        FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true);
    }
}
