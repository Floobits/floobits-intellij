package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;
import floobits.common.BrowserOpener;
import floobits.utilities.Flog;

import java.net.URI;
import java.net.URISyntaxException;

public class GoToHelp extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        BrowserOpener browserOpener = BrowserOpener.getInstance();
        if(!browserOpener.isBrowserSupported()) {
            Flog.error("Browser not supported on this platform, couldn't open help.");
            return;
        }
        URI uri;
        try {
            uri = new URI("https://floobits.com/help/plugins/intellij");
        } catch (URISyntaxException error) {
            Flog.error(error);
            return;
        }
        Project project = e.getProject();
        FloobitsPlugin floobitsPlugin;
        if (project != null) {
            floobitsPlugin = ServiceManager.getService(project, FloobitsPlugin.class);
        } else {
            floobitsPlugin = ServiceManager.getService(FloobitsPlugin.class);
        }

        browserOpener.openInBrowser(uri, "Click here to go to our IntelliJ IDEA help.", floobitsPlugin.context);
    }
}
