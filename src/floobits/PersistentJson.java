package floobits;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.FileUtils;


class Workspace implements Serializable {
    String url;
    String path;

    Workspace(String url, String path) {
        this.url = url;
        this.path = path;
    }

    public void clean() {
      if (!this.url.endsWith("/")) {
          this.url += "/";
      }
    }
}

public class PersistentJson {
    public Map<String, Map<String, Workspace>> workspaces = new HashMap<String, Map<String,Workspace>>();
    public Boolean auto_generated_account = false;
    public Boolean disable_account_creation = true;
    public ArrayList<Workspace> recent_workspaces = new ArrayList<Workspace>();

    public void addWorkspace(FlooUrl flooUrl, String path) {
        Map<String, Workspace> workspaces = this.workspaces.get(flooUrl.owner);
        if (workspaces == null) {
            workspaces = new HashMap<String, Workspace>();
            this.workspaces.put(flooUrl.owner, workspaces);
        }
        Workspace workspace = workspaces.get(flooUrl.workspace);
        if (workspace == null) {
            workspace = new Workspace(flooUrl.toString(), path);
            workspaces.put(flooUrl.workspace, workspace);
        } else {
            workspace.path = path;
            workspace.url = flooUrl.toString();
        }
        this.recent_workspaces.add(workspace);
        HashSet<String> seen = new HashSet<String>();
        ArrayList<Workspace> unique = new ArrayList<Workspace>();
        for (Workspace w : this.recent_workspaces) {
            w.clean();
            if (seen.contains(w.url)) {
                continue;
            }
            seen.add(w.url);
            unique.add(w);
        }
        this.recent_workspaces = unique;
    }

    public void save ()  {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            FileUtils.write(getFile(), gson.toJson(this), "UTF-8");
        } catch (Exception e) {
            Flog.error(e);
        }
    }

    public static File getFile() {
        return new File(FilenameUtils.concat(Shared.baseDir, "persistent.json"));
    }

    public static PersistentJson getInstance() {
        String s;
        try {
            s = FileUtils.readFileToString(getFile(), "UTF-8");
        } catch (Exception e) {
            Flog.error(e);
            s = "{}";
        }
        return new Gson().fromJson(s, PersistentJson.class);
    }
}
