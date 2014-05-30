package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.common.PersistentJson;
import floobits.common.Settings;
import floobits.dialogs.CreateAccount;

public abstract class RequiresAccountAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        PersistentJson p = PersistentJson.getInstance();
        if (Settings.canFloobits()){
            CreateAccount createAccount1 = new CreateAccount(null);
            createAccount1.createCenterPanel();
            createAccount1.show();
        } else {
            actionPerformedHasAccount(e);
        }
    }

    protected abstract void actionPerformedHasAccount(AnActionEvent e);
}
