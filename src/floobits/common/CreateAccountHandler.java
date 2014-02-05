package floobits.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;
import floobits.common.protocol.send.NewAccount;
import floobits.handlers.FlooHandler;
import floobits.utilities.Flog;

import java.util.Map;

/**
 * Created by kans on 1/28/14.
 */
public class CreateAccountHandler extends ConnectionInterface {
    protected FlooConn conn;
    protected Project project;


    public static void createAccount(Project project) {
        if (FlooHandler.isJoined) {
            FlooHandler flooHandler = FloobitsPlugin.getHandler();
            if (flooHandler == null) {
                return;
            }
            flooHandler.status_message("You already have an account and are connected with it.");
            return;
        }
        CreateAccountHandler createAccountHandler = new CreateAccountHandler();
        createAccountHandler.create(project);
    }


    public void create(Project project) {
        this.project = project;
        url = new FlooUrl(Shared.defaultHost, null, null, Shared.defaultPort, true);
        conn = new FlooConn(this);
        conn.start();
    }

    @Override
    public void on_data(String name, JsonObject obj) {
        Flog.info("on_data %s %s", obj, name);
        Settings settings = new Settings(project);
        for (Map.Entry<String, JsonElement> thing : obj.entrySet()) {
            settings.set(thing.getKey(), thing.getValue().getAsString());
        }
        if (!settings.isComplete()) {
            Utils.error_message("Can't create an account at this time.", project);
            shutDown();
            return;
        }
        settings.write();
        PersistentJson p = PersistentJson.getInstance();
        p.auto_generated_account = true;
        p.disable_account_creation = true;
        p.save();
        shutDown();
        // TODO: Show welcome message.
        Flog.info("All setup");
    }

    public void shutDown() {
        this.conn.shutDown();
    }

    @Override
    public void on_connect() {
        Flog.warn("Connected.");
        this.conn.write(new NewAccount());
    }
}
