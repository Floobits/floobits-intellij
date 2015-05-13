package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FloobitsPlugin;

public class FloobitsWindow extends CanFloobits {

    @Override
    public void actionPerformed(AnActionEvent e) {
        FloobitsPlugin floobitsPlugin = FloobitsPlugin.getInstance(e.getProject());
        floobitsPlugin.context.openChat();
    }
}
