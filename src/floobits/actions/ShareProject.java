package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;

public class ShareProject extends CanFloobits {

    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null ) {
            return;
        }
        final String project_path = project.getBasePath();
        FloobitsPlugin.getInstance(project).context.shareProject(false, project_path);
    }
}
