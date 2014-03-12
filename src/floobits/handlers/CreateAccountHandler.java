package floobits.handlers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import floobits.FlooContext;
import floobits.common.*;
import floobits.common.protocol.send.NewAccount;
import floobits.utilities.Flog;

import java.util.Map;

public class CreateAccountHandler extends BaseHandler {

    public CreateAccountHandler(FlooContext context) {
        super(context);
    }

    public void go() {
        url = new FlooUrl(Constants.defaultHost, null, null, Constants.defaultPort, true);
        conn = new FlooConn(this);
        conn.start();
    }

    @Override
    public void on_data(String name, JsonObject obj) {
        Flog.info("on_data %s %s", obj, name);
        Settings settings = new Settings(context);
        for (Map.Entry<String, JsonElement> thing : obj.entrySet()) {
            settings.set(thing.getKey(), thing.getValue().getAsString());
        }
        PersistentJson p = PersistentJson.getInstance();
        if (!settings.isComplete()) {
            p.disable_account_creation = true;
            p.save();
            context.errorMessage("Can't create an account at this time.");
            context.shutdown();
            return;
        }
        settings.write();
        p.auto_generated_account = true;
        p.disable_account_creation = true;
        p.save();
        // TODO: Show welcome message.
        context.statusMessage(String.format("Successfully created new Floobits account with username %s. You can now share a project or join a workspace.", settings.get("username")), false);
        Flog.info("All setup");
        context.shutdown();
    }

    @Override
    public void on_connect() {
        Flog.warn("Connected.");
        conn.write(new NewAccount());
    }
}
