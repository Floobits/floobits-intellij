package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.FloobitsPlugin;
import floobits.common.Settings;
import floobits.impl.ContextImpl;
import floobits.utilities.Flog;

import java.io.File;
import java.io.IOException;

public class OpenFloorc extends AnAction {
    public void actionPerformed(AnActionEvent actionEvent) {
        Project project = actionEvent.getProject();

        ContextImpl context;
        if (project == null) {
            context = ServiceManager.getService(FloobitsPlugin.class).context;
        } else {
            context = ServiceManager.getService(project, FloobitsPlugin.class).context;
        }
        File file = new File(Settings.floorcJsonPath);
        if (!file.exists()) {
            boolean created;
            try {
                created = file.createNewFile();
            } catch (IOException e) {
                context.errorMessage("Can't create ~/.floorc.json");
                Flog.error(e);
                return;
            }
            if (!created) {
                context.errorMessage("Can't create ~/.floorc.json");
                return;
            }
        }
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(Settings.floorcJsonPath);
        if (virtualFile == null) {
            context.errorMessage("No virtual file");
            return;
        }
        if (project == null) {
            context.errorMessage("Can't open ~/.floorc.json");
            return;
        }
        OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, virtualFile);
        FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true);
    }
}
