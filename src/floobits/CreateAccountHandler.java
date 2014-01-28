package floobits;


import com.google.gson.JsonObject;

import java.net.MalformedURLException;

public class CreateAccountHandler extends ConnectionInterface {
    protected FlooConn conn;

    public void createAccount () {
        url = new FlooUrl(Shared.defaultHost, null, null, Shared.defaultPort, true);
        conn = new FlooConn(this);
        conn.start();
    }

    @Override
    void on_data(String name, JsonObject obj) throws Exception {
        Flog.info("on_data");
    }

    @Override
    void on_connect() {
        Flog.warn("Connected.");
    }
}
