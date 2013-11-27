package floobits;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.*;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.FileUtils;


class Workspace implements Serializable {
    String url;
    String path;
}

class PersistentJson {
    public JsonObject json;
    public File f;
    public Map<String, Map<String, Workspace>> workspaces;

    public PersistentJson () {
        String s;
        try {
            this.f = new File(FilenameUtils.concat(Shared.baseDir, "persistent.json"));
            s = FileUtils.readFileToString(this.f, "UTF-8");
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
        FileUtils.write(this.f, new Gson().toJson(this.json), "UTF-8");
    }
}
