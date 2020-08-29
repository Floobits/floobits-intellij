package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;

public class FloobitsWindow extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        FloobitsPlugin floobitsPlugin = ServiceManager.getService(project, FloobitsPlugin.class);
        if (floobitsPlugin != null) {
            floobitsPlugin.context.toggleFloobitsWindow();
        }
    }
}
