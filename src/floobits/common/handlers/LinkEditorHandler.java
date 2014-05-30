package floobits.common.handlers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import floobits.FlooContext;
import floobits.common.*;
import floobits.common.protocol.send.FlooRequestCredentials;
import floobits.utilities.Flog;

import java.awt.*;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class LinkEditorHandler extends BaseHandler {
    protected String token;
    private String host;

    public LinkEditorHandler(FlooContext context) {
        super(context);
        UUID uuid = UUID.randomUUID();
        token = String.format("%040x", new BigInteger(1, uuid.toString().getBytes()));
        host = url.host;
    }

    public void go() {
        url = new FlooUrl(host, null, null, Constants.defaultPort, true);
        conn = new Connection(this);
        conn.start();
        isJoined = true;
        openBrowser();
    }


    @Override
    public void on_data(String name, JsonObject obj) {
        if (!name.equals("credentials")) {
            return;
        }
        FloorcJson floorcJson = Settings.get();
        HashMap<String, String> auth_host = floorcJson.auth.get(host);
        if (auth_host == null) {
            auth_host = new HashMap<String, String>();
            floorcJson.auth.put(host, auth_host);
        }
        JsonObject credentials = (JsonObject) obj.get("credentials");
        for (Map.Entry<String, JsonElement> thing : credentials.entrySet()) {
            String key = thing.getKey();
            if (key.equals("name")) {
                continue;
            }
            auth_host.put(key, thing.getValue().getAsString());
        }

        if (Settings.isComplete(auth_host)) {
            Settings.write(context, floorcJson);
            context.statusMessage(String.format("Your account, %s, was successfully retrieved.  You can now share a project or join a workspace.", auth_host.get("username")), false);
        } else {
            context.errorMessage("Something went wrong while receiving data, please contact Floobits support.");
        }
        context.shutdown();
    }

    protected void openBrowser() {
        if(!Desktop.isDesktopSupported()) {
            Utils.errorMessage("Floobits can't use a browser on this system.", context.project);
            context.shutdown();
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(String.format("https://%s/dash/link_editor/intellij/%s/", host, token)));
        } catch (IOException error) {
            context.shutdown();
            Flog.warn(error);
        } catch (URISyntaxException error) {
            context.shutdown();
            Flog.warn(error);
        }
    }

    @Override
    public void on_connect() {
        Flog.warn("Connected.");
        conn.write(new FlooRequestCredentials(token));
    }
}
