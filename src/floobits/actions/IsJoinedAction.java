package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FlooContext;
import floobits.FloobitsPlugin;
import floobits.handlers.FlooHandler;

public abstract class IsJoinedAction extends AnAction {

    public abstract void actionPerformed(AnActionEvent e, FlooHandler flooHandler);

    @Override
    public void actionPerformed(AnActionEvent e) {
        FlooContext instance = FloobitsPlugin.getInstance(e.getProject()).context;
        FlooHandler flooHandler = instance.getFlooHandler();
        if (flooHandler == null) {
            return;
        }
        actionPerformed(e, flooHandler);
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);    //To change body of overridden methods use File | Settings | File Templates.
        e.getPresentation().setEnabled(FloobitsPlugin.getInstance(e.getProject()).context.isJoined());
    }
}