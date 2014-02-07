package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FlooContext;
import floobits.FloobitsApplication;
import floobits.FloobitsPlugin;
import floobits.common.DotFloo;
import floobits.common.FlooUrl;
import floobits.utilities.Flog;

import javax.swing.*;


public class JoinWorkspace extends AnAction {

    public void actionPerformed(AnActionEvent e) {
        String url = "https://floobits.com/";
//        Flog.info(inputValue);
        FlooContext context = FloobitsPlugin.getInstance(e.getProject()).context;
        FlooUrl floourl = DotFloo.read(context.project.getBasePath());
        if (floourl != null) {
            url = floourl.toString();
        }
        String inputValue = JOptionPane.showInputDialog("Workspace URL", url);
        if (inputValue == null) {
            return;
        }

        FloobitsApplication.self.joinWorkspace(context, inputValue);
    }
}
