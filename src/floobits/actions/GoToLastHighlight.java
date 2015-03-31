package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FloobitsPlugin;

public class GoToLastHighlight extends RequiresAccountAction {

    @Override
    protected void actionPerformedHasAccount(AnActionEvent e) {
        FloobitsPlugin floobitsPlugin = FloobitsPlugin.getInstance(e.getProject());
        floobitsPlugin.context.iFactory.goToLastHighlight();
    }
}
