package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FloobitsApplication;

import javax.swing.*;

public class QuickStartJoin extends RequiresAccountAction {
    public void actionPerformedHasAccount(AnActionEvent e) {
        String inputValue = JOptionPane.showInputDialog("Workspace URL", "https://floobits.com/");
        if (inputValue == null) {
            return;
        }

        FloobitsApplication.self.joinWorkspace(inputValue);
    }
}
