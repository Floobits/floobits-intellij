package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FloobitsPlugin;

public class FloobitsWindow extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        FloobitsPlugin floobitsPlugin = FloobitsPlugin.getInstance(e.getProject());
        if (floobitsPlugin != null) {
            floobitsPlugin.context.toggleFloobitsWindow();
        }
    }
}
