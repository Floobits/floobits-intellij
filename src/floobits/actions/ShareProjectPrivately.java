package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;
import floobits.impl.ContextImpl;

public class ShareProjectPrivately extends IsBaseProjectPath {
    public void actionPerformed(AnActionEvent actionEvent, Project project, FloobitsPlugin plugin, ContextImpl context) {
        if (project == null ) {
            return;
        }
        final String project_path = project.getBasePath();
        FloobitsPlugin.getInstance(project).context.shareProject(true, project_path);
    }
}
