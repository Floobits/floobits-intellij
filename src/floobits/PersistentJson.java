package floobits;

import java.io.*;
import java.util.HashMap;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.*;

import floobits.Shared;
import floobits.Utils;

class Workspace implements Serializable {
    String url;
    String path;
}

class Owner implements Serializable {
    static HashMap<String, Workspace> name;
}

class Workspaces implements Serializable {
    HashMap<String, Owner> workspaces;
}

class PersistentJson {
    public JsonObject json;
    public Workspaces workspaces;
    public String path;

    public PersistentJson () {
        String path;
        String s;
        try {
            this.path = Utils.pathJoin(Shared.baseDir, "persistent.json");
            s = Utils.readFile(this.path);
        } catch (Exception e) {
            s = "{}";
        }
        this.json = new Gson().fromJson(s, JsonObject.class);
        this.workspaces = new Gson().fromJson(this.json.getAsJsonObject("workspaces"), Workspaces.class);
    }

    // TODO: don't delete everything except workspaces key in json
    public void write () throws IOException {
        Utils.writeFile(this.path, new Gson().toJson(this.json));
    }
}
