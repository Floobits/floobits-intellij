package floobits.handlers;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

    public LinkEditorHandler() {
        UUID uuid = UUID.randomUUID();
        token = String.format("%040x", new BigInteger(1, uuid.toString().getBytes()));
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
        JsonObject credentials = (JsonObject) obj.get("credentials");
        for (Map.Entry<String, JsonElement> thing : credentials.entrySet()) {
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
