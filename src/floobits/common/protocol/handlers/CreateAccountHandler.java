package floobits.common.protocol.handlers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import floobits.common.*;
import floobits.common.interfaces.IContext;
import floobits.common.protocol.Connection;
import floobits.common.protocol.json.send.NewAccount;
import floobits.utilities.Flog;

import java.util.HashMap;
import java.util.Map;

public class CreateAccountHandler extends BaseHandler {

    private String host;
    private Runnable afterCreateAccount;

    public CreateAccountHandler(IContext context, String _host, Runnable runnable) {
        super(context);
        host = _host;
        afterCreateAccount = runnable;
        url = new FlooUrl(host, null, null, Constants.defaultPort, true);
    }

    public void go() {
        super.go();
        conn = new Connection(this);
        conn.start();
    }

    @Override
    public void _on_data(String name, JsonObject obj) {
        Flog.info("on_data %s %s", obj, name);
        if (!name.equals("create_user")) {
            return;
        }
        FloorcJson floorcJson = FloorcJson.getFloorcJsonFromSettings();
        HashMap<String, String> auth_host = floorcJson.auth.get(host);
        if (floorcJson.auth == null) {
            floorcJson.auth = new HashMap<String, HashMap<String, String>>();
        }
        if (auth_host == null) {
            auth_host = new HashMap<String, String>();
            floorcJson.auth.put(host, auth_host);
        }
        for (Map.Entry<String, JsonElement> thing : obj.entrySet()) {
            String key = thing.getKey();
            if (key.equals("name")) {
                continue;
            }
            auth_host.put(key, thing.getValue().getAsString());
        }
        PersistentJson p = PersistentJson.getInstance();
        Settings.write(context, floorcJson);
        p.auto_generated_account = true;
        p.disable_account_creation = true;
        p.save();
        context.statusMessage(String.format("Successfully created new Floobits account with username %s. " +
                "You can now share a project or join a workspace.", auth_host.get("username")));
        Flog.info("All setup");
        context.shutdown();

        context.mainThread(afterCreateAccount);
    }

    @Override
    public void on_connect() {
        Flog.error("Connected.");
        conn.setRetries(-1);
        conn.write(new NewAccount());
    }
}
