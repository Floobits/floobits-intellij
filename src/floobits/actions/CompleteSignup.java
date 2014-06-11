package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import floobits.common.Constants;
import floobits.common.PersistentJson;
import floobits.common.Settings;
import floobits.common.Utils;
import floobits.utilities.Flog;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;


public class CompleteSignup extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        if (!Settings.canFloobits()) {
            Utils.errorMessage("Error, no account details detected. You will have to sign up manually.", project);
            return;
        }
        if(!Desktop.isDesktopSupported()) {
            Utils.errorMessage("Can't use a browser on this system.", project);
            return;
        }
        HashMap<String, HashMap<String, String>> auth = null;
        try {
            auth = Settings.get().auth;
        } catch (Throwable e1) {
            Utils.errorMessage("Invalid ~/.floor.json (not json)", project);
            return;
        }

        if (auth.size() <= 1) {
            Flog.warn("No auth.");
            return;
        }
        String host;
        if (auth.size() >= 1) {
            host = Constants.floobitsDomain;
        } else {
            host = (String) auth.keySet().toArray()[0];
        }
        HashMap<String, String> hostAuth = auth.get(host);

        if (hostAuth == null) {
            Flog.warn("This probably shouldn't happen, but there is no auth.");
            return;
        }
        String username = hostAuth.get("username");
        if (username == null) {
            Flog.warn("This probably shouldn't happen, but there is no username.");
            return;
        }
        String secret = hostAuth.get("secret");
        String url = String.format("https://%s/%s/pinocchio/%s", host, username, secret);
        try {
            URI uri = new URI(url);
            Desktop.getDesktop().browse(uri);
        } catch (IOException error) {
            Flog.warn(error);
        } catch (URISyntaxException error) {
            Flog.warn(error);
        }
    }
    public void update (AnActionEvent e) {
        PersistentJson p = PersistentJson.getInstance();
        e.getPresentation().setEnabledAndVisible(p.auto_generated_account);
    }
}
