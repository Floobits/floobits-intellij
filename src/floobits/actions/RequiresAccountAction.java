package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import floobits.common.Settings;
import floobits.dialogs.CreateAccount;

public abstract class RequiresAccountAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        if (Settings.canFloobits()) {
            actionPerformedHasAccount(e);
        } else {
            Project project = e.getProject();
            CreateAccount createAccount1 = new CreateAccount(project);
            createAccount1.createCenterPanel();
            createAccount1.show();
        }
    }

    protected abstract void actionPerformedHasAccount(AnActionEvent e);
}
