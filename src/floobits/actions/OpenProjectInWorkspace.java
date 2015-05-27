package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import floobits.FloobitsApplication;
import floobits.FloobitsPlugin;
import floobits.common.*;
import floobits.dialogs.HandleNoWorkspaceJoin;
import floobits.impl.ContextImpl;
import floobits.utilities.Flog;

import java.net.MalformedURLException;
import java.util.Map;


public class OpenProjectInWorkspace extends CanFloobits {
    public void actionPerformed(AnActionEvent actionEvent, Project project, FloobitsPlugin plugin, ContextImpl context) {
        FlooUrl flooUrl = DotFloo.read(project.getBasePath());
        if (flooUrl != null) {
            FloobitsApplication.self.joinWorkspace(context, flooUrl.toString());
            return;
        }

        String project_path = context.project.getBasePath();
        PersistentJson persistentJson = PersistentJson.getInstance();
        for (Map.Entry<String, Map<String, Workspace>> i : persistentJson.workspaces.entrySet()) {
            Map<String, Workspace> workspaces = i.getValue();
            for (Map.Entry<String, Workspace> j : workspaces.entrySet()) {
                Workspace w = j.getValue();
                if (Utils.isSamePath(w.path, project_path)) {
                    try {
                        flooUrl = new FlooUrl(w.url);
                    } catch (MalformedURLException e) {
                        Flog.error(e);
                        continue;
                    }
                    FloobitsApplication.self.joinWorkspace(context, flooUrl.toString());
                    return;
                }
            }
        }
        HandleNoWorkspaceJoin dialog = new HandleNoWorkspaceJoin(context);
        dialog.createCenterPanel();
        dialog.show();
    }
}
