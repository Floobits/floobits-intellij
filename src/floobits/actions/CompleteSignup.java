package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import floobits.common.Settings;
import floobits.common.Shared;
import floobits.common.Utils;
import floobits.utilities.Flog;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


public class CompleteSignup extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        Settings settings = new Settings(project);
        if (!settings.isComplete()) {
            Utils.error_message("Error, no account details detected. You will have to sign up manually.", project);
            return;
        }
        if(!Desktop.isDesktopSupported()) {
            Utils.error_message("Can't use a browser on this system.", project);
            return;
        }
        String username = settings.get("username");
        if (username == null) {
            Flog.warn("This probably shouldn't happen, but there is no username.");
            return;
        }
        String secret = settings.get("secret");
        String url = String.format("https://%s/%s/pinocchio/%s/", Shared.defaultHost, username, secret);
        try {
            URI uri = new URI(url);
            Desktop.getDesktop().browse(uri);
        } catch (IOException error) {
            Flog.warn(error);
        } catch (URISyntaxException error) {
            Flog.warn(error);
        }
    }
}
