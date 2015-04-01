package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FloobitsApplication;
import floobits.FloobitsPlugin;
import floobits.common.DotFloo;
import floobits.common.FlooUrl;
import floobits.common.protocol.handlers.FlooHandler;
import floobits.impl.ContextImpl;
import floobits.utilities.Flog;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


public class OpenSettingsInBrowser extends CanFloobits {
    public void actionPerformed(AnActionEvent actionEvent) {
        ContextImpl context = FloobitsPlugin.getInstance(actionEvent.getProject()).context;
        String path = context.project.getBasePath();
        FlooUrl flooUrl = DotFloo.read(path);
        if (flooUrl == null) {
            context.errorMessage(String.format("Could not determine the Floobits workspace for %s, did you create it and join it previously?", path));
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(String.format("%s/settings", flooUrl.toString())));
        } catch (IOException e) {
            Flog.info("Couldn't open settings in browser", e);
        } catch (URISyntaxException e) {
            Flog.info("Couldn't open settings in browser", e);
        }
    }
}
