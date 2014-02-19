package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FlooContext;
import floobits.FloobitsApplication;
import floobits.FloobitsPlugin;
import floobits.common.*;
import floobits.utilities.Flog;

import javax.swing.*;
import java.net.MalformedURLException;
import java.util.Map;

/**
 * Created by kans on 2/18/14.
 */
public class OpenProjectInWorkspace extends AnAction {
    public void actionPerformed(AnActionEvent actionEvent) {
        FlooContext context = FloobitsPlugin.getInstance(actionEvent.getProject()).context;
        FlooUrl flooUrl = DotFloo.read(context.project.getBasePath());
        if (flooUrl != null && API.workspaceExists(flooUrl, context)) {
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
                        Flog.warn(e);
                        continue;
                    }
                    if (API.workspaceExists(flooUrl, context)) {
                        FloobitsApplication.self.joinWorkspace(context, flooUrl.toString());
                        return;
                    }
                }
            }
        }
        context.error_message("This project doesn't seem to be associated with a Floobits workspace.");
    }
}
