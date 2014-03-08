package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FloobitsPlugin;

public class LeaveWorkspace extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        FloobitsPlugin.getInstance(e.getProject()).context.shutdown();
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
