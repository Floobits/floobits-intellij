package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;

/**
 * User: bjorn
 * Date: 2/14/14
 * Time: 12:08 PM
 */
public class ShareProjectPrivately extends CanFloobits {
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null ) {
            return;
        }
        final String project_path = project.getBasePath();
        FloobitsPlugin.getInstance(project).context.shareProject(true, project_path);
    }
}
