package floobits.common;

import com.google.gson.JsonObject;

abstract public class ConnectionInterface {
    public FlooUrl url;

    public abstract void on_data(String name, JsonObject obj) throws Exception;
    public abstract void on_connect();

    public FlooUrl getUrl() {
        return url;
    }
}
