package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;
import floobits.handlers.FlooHandler;

public abstract class IsJoinedAction extends AnAction {

    public void actionPerformed(AnActionEvent e, FlooHandler flooHandler) {

    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        FloobitsPlugin instance = FloobitsPlugin.getInstance(e.getProject());
        FlooHandler flooHandler = instance.getFlooHandler();
        if (flooHandler == null) {
            return;
        }
        actionPerformed(e, flooHandler);
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);    //To change body of overridden methods use File | Settings | File Templates.
        Project project = e.getProject();
        FloobitsPlugin floobitsPlugin = project != null ? project.getComponent(FloobitsPlugin.class) : null;
        e.getPresentation().setEnabled(floobitsPlugin != null && floobitsPlugin.isJoined());
    }
}