package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;

public class GoToLastHighlight extends RequiresAccountAction {

    @Override
    protected void actionPerformedHasAccount(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        FloobitsPlugin floobitsPlugin = project.getService(FloobitsPlugin.class);
        if (floobitsPlugin == null) {
            return;
        }
        floobitsPlugin.context.iFactory.goToLastHighlight();
    }
}
