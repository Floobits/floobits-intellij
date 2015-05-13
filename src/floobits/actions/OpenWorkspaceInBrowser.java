package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;
import floobits.common.DotFloo;
import floobits.common.FlooUrl;
import floobits.impl.ContextImpl;
import floobits.utilities.Flog;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class OpenWorkspaceInBrowser extends CanFloobits {
    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        if (project == null) {
            return;
        }
        ContextImpl context = FloobitsPlugin.getInstance(project).context;
        String path = context.project.getBasePath();
        FlooUrl flooUrl = DotFloo.read(path);
        if (flooUrl == null) {
            context.errorMessage(String.format("Could not determine the Floobits workspace for %s, did you create it and join it previously?", path));
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(flooUrl.toString()));
        } catch (IOException e) {
            Flog.info("Couldn't open settings in browser", e);
        } catch (URISyntaxException e) {
            Flog.info("Couldn't open settings in browser", e);
        }
    }
}
