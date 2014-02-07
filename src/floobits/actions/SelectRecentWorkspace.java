package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.common.PersistentJson;
import floobits.common.Workspace;

import java.util.LinkedList;

public class SelectRecentWorkspace extends AnAction {

    public void actionPerformed(AnActionEvent e) {
        PersistentJson persistentJson = PersistentJson.getInstance();
        LinkedList<String> recent = new LinkedList<String>();
        for (Workspace workspace : persistentJson.recent_workspaces) {
            recent.add(workspace.url);
        }
        floobits.dialogs.SelectRecentWorkspace selectRecentWorkspace = new floobits.dialogs.SelectRecentWorkspace(e.getProject(), recent);
        selectRecentWorkspace.createCenterPanel();
        selectRecentWorkspace.show();
    }
}