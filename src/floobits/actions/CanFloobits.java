package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;
import floobits.common.Settings;
import floobits.dialogs.CreateAccount;
import floobits.impl.ContextImpl;
import floobits.utilities.Flog;


abstract public class CanFloobits extends AnAction {

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        FloobitsPlugin floobitsPlugin = FloobitsPlugin.getInstance(e.getProject());
        if (floobitsPlugin == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        e.getPresentation().setEnabled(Settings.canFloobits());
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            Flog.warn("Tried to perform an action but there is no project");
            return;
        }
        FloobitsPlugin plugin = FloobitsPlugin.getInstance(project);
        if (plugin == null || !Settings.canFloobits()) {
            showCreateAccount(project);
            return;
        }
        ContextImpl context = plugin.context;
        actionPerformed(e, project, plugin, context);
    }

    private void showCreateAccount(Project project) {
        Flog.warn("Showing create account because there's no plugin.");
        CreateAccount createAccountDialog = new CreateAccount(project);
        createAccountDialog.createCenterPanel();
        createAccountDialog.show();
    }

    abstract public void actionPerformed(AnActionEvent e, Project project, FloobitsPlugin plugin, ContextImpl context);
}
