package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.FloobitsPlugin;
import floobits.utilities.Flog;

public class ShareProject extends IsBaseProjectPath {

    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null ) {
            return;
        }
        final String project_path = project.getBasePath();
        FloobitsPlugin.getInstance(project).context.shareProject(false, project_path);
    }
}
