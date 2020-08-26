package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;

public class ClearHighlights extends RequiresAccountAction {

    @Override
    protected void actionPerformedHasAccount(AnActionEvent e) {
        Project project = e.getProject();
        FloobitsPlugin floobitsPlugin;
        if (project == null) {
            floobitsPlugin = ServiceManager.getService(FloobitsPlugin.class);
        } else {
            floobitsPlugin = ServiceManager.getService(project, FloobitsPlugin.class);
        }

        if (floobitsPlugin != null) {
            floobitsPlugin.context.iFactory.clearHighlights();
        }
    }
}
