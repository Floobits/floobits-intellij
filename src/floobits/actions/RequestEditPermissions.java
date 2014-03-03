package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FlooContext;
import floobits.FloobitsPlugin;
import floobits.handlers.FlooHandler;

/**
 * User: bjorn
 * Date: 2/27/14
 * Time: 1:06 PM
 */
public class RequestEditPermissions extends IsJoinedAction {
    @Override
    public void actionPerformed(AnActionEvent e, FlooHandler flooHandler) {
        flooHandler.sendEditRequest();
    }


    @Override
     public void update(AnActionEvent e) {
        super.update(e);
        FlooContext instance = FloobitsPlugin.getInstance(e.getProject()).context;
        FlooHandler flooHandler = instance.getFlooHandler();
        if (flooHandler == null) {
            return;
        }
        e.getPresentation().setEnabled(!flooHandler.can("patch"));
    }
}
