package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;
import floobits.common.interfaces.IContext;
import floobits.common.protocol.handlers.FlooHandler;
import floobits.utilities.Flog;

// TODO: figure out why we didn't extend CanFloobits
public class LeaveWorkspace extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            Flog.log("no project to find FloobitsPlugin, can not leave, sorry.");
            return;
        }
        FloobitsPlugin floobitsPlugin = ServiceManager.getService(project, FloobitsPlugin.class);
        IContext context = floobitsPlugin.context;
        FlooHandler handler = context.getFlooHandler();
        if (handler == null) {
            context.errorMessage("You are not connected to a Floobits workspace.");
        }
        // Shut it down in all cases, because shutdown clears chat and does other things beyond disconnecting.
        context.shutdown();
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        Project project = e.getProject();
        if (project == null) {
            Flog.log("no project to find FloobitsPlugin, can not update LeaveWorkspace, sorry.");
            return;
        }
        FloobitsPlugin floobitsPlugin = project.getServiceIfCreated(FloobitsPlugin.class);
        if (floobitsPlugin == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        e.getPresentation().setEnabled(floobitsPlugin.context.isJoined());
    }
}
