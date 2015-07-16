package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;
import floobits.common.Utils;
import floobits.utilities.Flog;
import floobits.utilities.IntelliUtils;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class GoToHelp extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        if(!Desktop.isDesktopSupported()) {
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
        FloobitsPlugin plugin = FloobitsPlugin.getInstance(project);
        IntelliUtils.openInBrowser(uri, "Click here to go to our IntelliJ IDEA help.", plugin.context);
    }
}
