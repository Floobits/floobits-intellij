package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;
import floobits.impl.ContextImpl;

public class ShareProject extends IsBaseProjectPath {

    public void actionPerformed(AnActionEvent actionEvent, Project project, FloobitsPlugin plugin, ContextImpl context) {
        final String project_path = project.getBasePath();
        FloobitsPlugin floobitsPlugin = ServiceManager.getService(project, FloobitsPlugin.class);
        floobitsPlugin.context.shareProject(false, project_path);
    }
}
