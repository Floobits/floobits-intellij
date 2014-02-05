package floobits.handlers;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;
import floobits.utilities.Flog;
import floobits.common.*;
import floobits.common.protocol.send.FlooRequestCredentials;

import java.awt.*;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;


public class LinkEditorHandler extends ConnectionInterface {
    protected FlooConn conn;
    protected String token;

    public LinkEditorHandler(Project project) {
        this.project = project;
        UUID uuid = UUID.randomUUID();
        token = String.format("%040x", new BigInteger(1, uuid.toString().getBytes()));
    }

    public static void linkEditor(Project project) {
        if (FlooHandler.isJoined) {
            Utils.status_message("You already have an account and are connected with it.", project);
            FlooHandler floohandler = FloobitsPlugin.getHandler();
            floohandler.shutDown();
        }
        LinkEditorHandler linkEditorHandler = new LinkEditorHandler(project);
        linkEditorHandler.link();
    }

    public void link() {
        url = new FlooUrl(Shared.defaultHost, null, null, Shared.defaultPort, true);
        conn = new FlooConn(this);
        conn.start();
        openBrowser();
    }


    @Override
    public void on_data(String name, JsonObject obj) {
        if (!name.equals("credentials")) {
            return;
        }
        Settings settings = new Settings(project);
        JsonObject credentials = (JsonObject) obj.get("credentials");
        for (Map.Entry<String, JsonElement> thing : credentials.entrySet()) {
            settings.set(thing.getKey(), thing.getValue().getAsString());
        }
        if (settings.isComplete()) {
            settings.write();
        } else {
            Utils.error_message("Something went wrong while receiving data, please contact Floobits support.", project);
        }
        shutDown();
    }

    protected void openBrowser() {
        if(!Desktop.isDesktopSupported()) {
            Utils.error_message("Floobits can't use a browser on this system.", project);
            shutDown();
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(String.format("https://%s/dash/link_editor/%s/", Shared.defaultHost, token)));
        } catch (IOException error) {
            shutDown();
            Flog.warn(error);
        } catch (URISyntaxException error) {
            shutDown();
            Flog.warn(error);
        }
    }

    public void shutDown() {
        this.conn.shutDown();
    }

    @Override
    public void on_connect() {
        Flog.warn("Connected.");
        this.conn.write(new FlooRequestCredentials(token));
    }
}
