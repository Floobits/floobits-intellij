package floobits;

import com.intellij.openapi.actionSystem.AnActionEvent;

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
            Flog.error(error);
        } catch (URISyntaxException error) {
            Flog.error(error);
        }
    }
}
