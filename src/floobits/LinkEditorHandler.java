package floobits;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import floobits.common.*;
import floobits.common.protocol.send.FlooRequestCredentials;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;


public class LinkEditorHandler extends ConnectionInterface {
    protected FlooConn conn;
    protected String token;

    public LinkEditorHandler() {
        token = ""; // Make a token.
    }

    public static void linkEditor() {
        if (FlooHandler.isJoined) {
            Flog.throwAHorribleBlinkingErrorAtTheUser("You already have an account and are connected with it.");
            FlooHandler floohandler = FlooHandler.getInstance();
            floohandler.shutDown();
        }
        LinkEditorHandler linkEditorHandler = new LinkEditorHandler();
        linkEditorHandler.link();
    }

    public void link() {
        url = new FlooUrl(Shared.defaultHost, null, null, Shared.defaultPort, true);
        conn = new FlooConn(this);
        conn.start();
        openBrowser();
    }


    @Override
    public void on_data(String name, JsonObject obj) throws Exception {
        if (!name.equals("credentials")) {
            return;
        }
        Settings settings = new Settings();
        for (Map.Entry<String, JsonElement> thing : obj.entrySet()) {
            settings.set(thing.getKey(), thing.getValue().getAsString());
        }
        if (settings.isComplete()) {
            settings.write();
        } else {
            Flog.throwAHorribleBlinkingErrorAtTheUser("Something went wrong, please contact support.");
        }
        shutDown();
    }

    protected void openBrowser() {
        if(!Desktop.isDesktopSupported()) {
            Flog.throwAHorribleBlinkingErrorAtTheUser("Can't use a browser on this system.");
            shutDown();
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(String.format("http://%s/dash/link_editor/%s/", Shared.defaultHost, token)));
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
