package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.utilities.Flog;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bjorn on 4/23/14.
 */
public class GoToHelp extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        if(!Desktop.isDesktopSupported()) {
            Flog.warn("Browser not supported on this platform, couldn't open help.");
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI("https://floobits.com/help/plugins/intellij"));
        } catch (IOException error) {
            Flog.warn(error);
        } catch (URISyntaxException error) {
            Flog.warn(error);
        }
    }
}
