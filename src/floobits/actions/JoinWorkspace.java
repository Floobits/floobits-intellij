package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FloobitsPlugin;
import floobits.common.DotFloo;
import floobits.common.FlooUrl;
import floobits.dialogs.JoinWorkspaceDialog;
import floobits.impl.ContextImpl;



public class JoinWorkspace extends CanFloobits {

    public void actionPerformed(AnActionEvent e) {
        String url = "https://floobits.com/";
        FloobitsPlugin floobitsPlugin = FloobitsPlugin.getInstance(e.getProject());
        if (floobitsPlugin == null) {
            return;
        }
        ContextImpl context = floobitsPlugin.context;
        FlooUrl floourl = DotFloo.read(context.project.getBasePath());
        if (floourl != null) {
            url = floourl.toString();
        }

        JoinWorkspaceDialog dialog = new JoinWorkspaceDialog(url);
        dialog.createCenterPanel();
        dialog.show();
    }
}
