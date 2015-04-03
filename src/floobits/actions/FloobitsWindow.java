package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;
import floobits.common.EditorEventHandler;

public class FloobitsWindow extends CanFloobits {

    @Override
    public void actionPerformed(AnActionEvent e) {
        FloobitsPlugin floobitsPlugin = FloobitsPlugin.getInstance(e.getProject());
        floobitsPlugin.context.openChat();
    }
}
