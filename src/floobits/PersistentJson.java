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
import floobits.Flog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import floobits.Shared;
import floobits.Utils;

class Workspace implements Serializable {
    String url;
    String path;
}

class PersistentJson {

    public JsonObject json;
    public File path;
    public Map<String, Map<String, Workspace>> workspaces;

    public PersistentJson () {
        String s;
        try {
            this.path = new File(FilenameUtils.concat(Shared.baseDir, "persistent.json"));
            s = FileUtils.readFileToString(this.path, "UTF-8");
        } catch (Exception e) {
            Flog.error(e);
            s = "{}";
        }

        this.json = (JsonObject)new JsonParser().parse(s);
        Type Workspaces = new TypeToken<Map<String, Map<String, Workspace>>>() { }.getType();
        this.workspaces = new Gson().fromJson(this.json.get("workspaces").getAsJsonObject(), Workspaces);
    }

    // TODO: don't delete everything except workspaces key in json
    public void write () throws IOException {
        // FileUtils.write(new File(absPath), text, "UTF-8");
        // Utils.writeFile(this.path, new Gson().toJson(this.json));
    }
}
