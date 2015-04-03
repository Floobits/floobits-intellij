package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FloobitsPlugin;
import floobits.common.interfaces.IContext;
import floobits.common.protocol.handlers.FlooHandler;

public class LeaveWorkspace extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        IContext context = FloobitsPlugin.getInstance(e.getProject()).context;
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
        FloobitsPlugin floobitsPlugin = FloobitsPlugin.getInstance(e.getProject());
        if (floobitsPlugin == null) {
            return;
        }
        e.getPresentation().setEnabled(floobitsPlugin.context.isJoined());
    }
}
