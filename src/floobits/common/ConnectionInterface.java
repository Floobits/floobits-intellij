package floobits.common;

import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;

abstract public class ConnectionInterface {
    public FlooUrl url;
    protected Project project;

    public abstract void on_data(String name, JsonObject obj);
    public abstract void on_connect();

    public Project getProject() {
        return project;
    }

    public FlooUrl getUrl() {
        return url;
    }
}
