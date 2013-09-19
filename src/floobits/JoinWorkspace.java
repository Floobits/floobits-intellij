package floobits;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import floobits.FloobitsPlugin;

public class JoinWorkspace extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        FloobitsPlugin.joinWorkspace();
    }
}