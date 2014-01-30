package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.utilities.Flog;
import floobits.handlers.FlooHandler;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class OpenWorkspaceInBrowser extends IsJoinedAction {
    @Override
    public void actionPerformed(AnActionEvent e, FlooHandler flooHandler) {
        if(!Desktop.isDesktopSupported()) {
            flooHandler.status_message("This version of java lacks to support to open your browser.");
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(flooHandler.url.toString()));
        } catch (IOException error) {
            Flog.warn(error);
        } catch (URISyntaxException error) {
            Flog.warn(error);
        }
    }
}