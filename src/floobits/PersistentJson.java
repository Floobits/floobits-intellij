package floobits;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.google.gson.Gson;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.FileUtils;


class Workspace implements Serializable {
    String url;
    String path;

    Workspace(String url, String path) {
        this.url = url;
        this.path = path;
    }
}

public class PersistentJson {
    public Map<String, Map<String, Workspace>> workspaces;
    public Boolean auto_generated_account;
    public Boolean disable_account_creation;
    public ArrayList<Workspace> recent_workspaces;

    public PersistentJson () {
        File f = new File(FilenameUtils.concat(Shared.baseDir, "persistent.json"));
    }

    public void addWorkspace(FlooUrl flooUrl, String path) {
        Map<String, Workspace> workspaces = this.workspaces.get(flooUrl.owner);
        if (workspaces == null) {
            workspaces = new HashMap<String, Workspace>();
        }
        Workspace workspace = workspaces.get(flooUrl.workspace);
        if (workspace == null) {
            workspace = new Workspace(flooUrl.toString(), path);
        } else {
            workspace.path = path;
            workspace.url = flooUrl.toString();
        }
        this.recent_workspaces.add(workspace);
        HashSet<Workspace> seen = new HashSet<Workspace>();
        ArrayList<Workspace> unique = new ArrayList<Workspace>();
        for (Workspace w : this.recent_workspaces) {
            if (seen.contains(w)) {
                continue;
            }
            seen.add(w);
            unique.add(w);
        }
        this.recent_workspaces = unique;
    }

    public void save ()  {
        try {
            FileUtils.write(getFile(), new Gson().toJson(this), "UTF-8");
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
