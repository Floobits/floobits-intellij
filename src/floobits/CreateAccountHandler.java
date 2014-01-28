package floobits;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationInfo;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.Map;

class FlooNewAccount implements Serializable {
    // TODO: Share this code with FlooAuth
    String name = "create_user";
    String username = System.getProperty("user.name");
    String client = ApplicationInfo.getInstance().getVersionName();
    public String platform = System.getProperty("os.name");
    public String version = "0.10";

    public FlooNewAccount() {
        // Do nothing.
    }
}

public class CreateAccountHandler extends ConnectionInterface {
    protected FlooConn conn;


    public static void createAccount() {
        if (FlooHandler.is_joined) {
            Flog.throwAHorribleBlinkingErrorAtTheUser("You already have an account and are connected with it.");
            return;
        }
        CreateAccountHandler createAccountHandler = new CreateAccountHandler();
        createAccountHandler.create();
    }


    public void create() {
        url = new FlooUrl(Shared.defaultHost, null, null, Shared.defaultPort, true);
        conn = new FlooConn(this);
        conn.start();
    }

    @Override
    void on_data(String name, JsonObject obj) throws Exception {
        Flog.info("on_data %s %s", obj, name);
        Settings settings = new Settings();
        for (Map.Entry<String, JsonElement> thing : obj.entrySet()) {
            settings.set(thing.getKey(), thing.getValue().getAsString());
        }
        if (!settings.isComplete()) {
            Flog.throwAHorribleBlinkingErrorAtTheUser("Can't create an account at this time.");
            shutDown();
            return;
        }
        settings.write();
        PersistentJson p = PersistentJson.getInstance();
        p.auto_generated_account = true;
        p.disable_account_creation = true;
        p.save();
        shutDown();
        // TODO: Show welcome message
        Flog.info("All setup");
    }

    public void shutDown() {
        this.conn.shutDown();
    }

    @Override
    void on_connect() {
        Flog.warn("Connected.");
        this.conn.write(new FlooNewAccount());
    }
}
