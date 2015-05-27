package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;
import floobits.common.PersistentJson;
import floobits.common.Workspace;
import floobits.impl.ContextImpl;

import java.util.LinkedList;

public class SelectRecentWorkspace extends CanFloobits {

    public static void joinRecent(Project project) {
        PersistentJson persistentJson = PersistentJson.getInstance();
        LinkedList<String> recent = new LinkedList<String>();
        for (Workspace workspace : persistentJson.recent_workspaces) {
            recent.add(workspace.url);
        }
        floobits.dialogs.SelectRecentWorkspace selectRecentWorkspace = new floobits.dialogs.SelectRecentWorkspace(project, recent);
        selectRecentWorkspace.createCenterPanel();
        selectRecentWorkspace.show();
    }

    public void actionPerformed(AnActionEvent actionEvent, Project project, FloobitsPlugin plugin, ContextImpl context) {
        joinRecent(project);
    }
}
