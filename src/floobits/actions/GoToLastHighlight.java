package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import floobits.FloobitsPlugin;

public class GoToLastHighlight extends RequiresAccountAction {

    @Override
    protected void actionPerformedHasAccount(AnActionEvent e) {
        FloobitsPlugin floobitsPlugin = ServiceManager.getService(e.getProject(), FloobitsPlugin.class);
        if (floobitsPlugin == null) {
            return;
        }
        floobitsPlugin.context.iFactory.goToLastHighlight();
    }
}
