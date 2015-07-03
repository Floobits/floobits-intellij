package floobits.actions;


import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FloobitsPlugin;
import floobits.common.interfaces.IContext;

import javax.swing.*;
import java.io.File;

public class ClearPersistentJSON extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        String message = "You are about to clear the Floobits cache located at ~/.floobits/persistent.json.\n";
        message += "Doing this could have adverse side effects if you've created your account via this plugin\n";
        message += "and haven't signed up on the website yet.";
        FloobitsPlugin floobitsPlugin = FloobitsPlugin.getInstance(e.getProject());
        IContext context = floobitsPlugin.context;
        int answer = JOptionPane.showConfirmDialog(null, message);
        if (answer == JOptionPane.YES_OPTION) {
            String homeDir = System.getProperty("user.home");
            File file = new File(String.format("%s/floobits/persistent.json", homeDir));
            boolean fileDeleted = file.delete();
            if (fileDeleted && context != null) {
                context.statusMessage("Cache cleared, ~/.floobits/persistent.json was deleted.");
            } else if (context != null) {
                context.errorMessage("Could not clear cache, could not delete ~/.floobits/persistent.json.");
            }
        } else if (context != null) {
            context.statusMessage("Canceled clearing the cache.");
        }
    }
}