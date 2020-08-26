package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import floobits.FloobitsApplicationService;

import javax.swing.*;

public class QuickStartJoin extends RequiresAccountAction {
    public void actionPerformedHasAccount(AnActionEvent e) {
        String inputValue = JOptionPane.showInputDialog("Workspace URL", "https://floobits.com/");
        if (inputValue == null) {
            return;
        }
        FloobitsApplicationService applicationService = ServiceManager.getService(FloobitsApplicationService.class);
        applicationService.joinWorkspace(inputValue);
    }
}
