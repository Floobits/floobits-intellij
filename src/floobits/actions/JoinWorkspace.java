package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.BaseContext;
import floobits.FloobitsApplication;
import floobits.FloobitsPlugin;
import floobits.common.DotFloo;
import floobits.common.FlooUrl;

import javax.swing.*;


public class JoinWorkspace extends CanFloobits {

    public void actionPerformed(AnActionEvent e) {
        String url = "https://floobits.com/";
        FloobitsPlugin floobitsPlugin = FloobitsPlugin.getInstance(e.getProject());
        if (floobitsPlugin == null) {
            return;
        }
        BaseContext context = floobitsPlugin.context;
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
