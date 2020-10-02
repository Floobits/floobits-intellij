package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;
import floobits.common.DotFloo;
import floobits.common.FlooUrl;
import floobits.dialogs.JoinWorkspaceDialog;
import floobits.impl.ContextImpl;



public class JoinWorkspace extends CanFloobits {

    public void actionPerformed(AnActionEvent actionEvent, Project project, FloobitsPlugin plugin, ContextImpl context) {
        if (plugin == null) {
            return;
        }
        String url = "https://floobits.com/";
        FlooUrl floourl = DotFloo.read(project.getBasePath());
        if (floourl != null) {
            url = floourl.toString();
        }

        JoinWorkspaceDialog dialog = new JoinWorkspaceDialog(url);
        dialog.createCenterPanel();
        dialog.show();
    }
}
