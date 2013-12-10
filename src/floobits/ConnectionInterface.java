package floobits;

import com.google.gson.JsonObject;

abstract public class ConnectionInterface {
    FlooUrl url;

    abstract void on_data(String name, JsonObject obj);
    abstract void on_connect();

    public FlooUrl getUrl() {
        return url;
    }
}
