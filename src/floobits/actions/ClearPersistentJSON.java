package floobits.actions;


import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import floobits.FloobitsPlugin;
import floobits.common.interfaces.IContext;

import javax.swing.*;
import java.io.File;

public class ClearPersistentJSON extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        String message = "You are about to clear the Floobits cache located at ~/.floobits/persistent.json.\n";
        message += "Doing this could have adverse side effects if you've created your account via this plugin\n";
        message += "and haven't signed up on the website yet.";
        FloobitsPlugin floobitsPlugin = ServiceManager.getService(FloobitsPlugin.class);
        IContext context = null;
        if (floobitsPlugin != null) {
            context = floobitsPlugin.context;
        }
        int answer = JOptionPane.showConfirmDialog(null, message);
        if (answer == JOptionPane.YES_OPTION) {
            String homeDir = System.getProperty("user.home");
            File file = new File(String.format("%s/floobits/persistent.json", homeDir));
            boolean fileDeleted = file.delete();
            if (context == null) {
                return;
            }
            if (fileDeleted) {
                context.statusMessage("Cache cleared, ~/.floobits/persistent.json was deleted.");
            } else {
                context.errorMessage("Could not clear cache, could not delete ~/.floobits/persistent.json.");
            }
        }
    }
}