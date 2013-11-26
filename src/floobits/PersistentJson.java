package floobits;

import java.io.*;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Map;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonParser;
import com.google.gson.*;
import com.intellij.openapi.diagnostic.Logger;

import floobits.Shared;
import floobits.Utils;

class Workspace implements Serializable {
    String url;
    String path;
}

class PersistentJson {
    private static Logger Log = Logger.getInstance(Listener.class);
    public JsonObject json;
    public String path;
    public Map<String, Map<String, Workspace>> workspaces;

    public PersistentJson () {
        String path;
        String s;
        try {
            this.path = Utils.pathJoin(Shared.baseDir, "persistent.json");
            s = Utils.readFile(this.path);
        } catch (Exception e) {
            Log.info(e);
            s = "{}";
        }

        this.json = (JsonObject)new JsonParser().parse(s);
        Type Workspaces = new TypeToken<Map<String, Map<String, Workspace>>>() { }.getType();
        this.workspaces = new Gson().fromJson(this.json.get("workspaces").getAsJsonObject(), Workspaces);
    }

    // TODO: don't delete everything except workspaces key in json
    public void write () throws IOException {
        Utils.writeFile(this.path, new Gson().toJson(this.json));
    }
}