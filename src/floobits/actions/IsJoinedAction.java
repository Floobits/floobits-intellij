package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FlooHandler;

public abstract class IsJoinedAction extends AnAction {

    public void actionPerformed(AnActionEvent e, FlooHandler flooHandler) {

    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        FlooHandler instance = FlooHandler.getInstance();
        if (instance == null) {
            return;
        }
        actionPerformed(e, instance);
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);    //To change body of overridden methods use File | Settings | File Templates.
        e.getPresentation().setEnabled(FlooHandler.isJoined);
    }
}