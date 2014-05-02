package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;

public class QuickStartJoinRecent extends RequiresAccountAction {
    public void actionPerformedHasAccount(AnActionEvent e) {
        SelectRecentWorkspace.joinRecent(null);
    }
}
