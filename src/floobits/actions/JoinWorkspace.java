package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FloobitsPlugin;
import floobits.utilities.Flog;

import javax.swing.*;


public class JoinWorkspace extends AnAction {

    public void actionPerformed(AnActionEvent e) {
        String inputValue = JOptionPane.showInputDialog("Workspace URL", "https://floobits.com/");
        if (inputValue == null) {
            return;
        }
        Flog.info(inputValue);
        FloobitsPlugin context = FloobitsPlugin.getInstance(e.getProject());
        context.joinWorkspace(e.getProject(), inputValue);
    }
}
