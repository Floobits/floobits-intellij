package floobits.common;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import floobits.utilities.Flog;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.FileUtils;


public class PersistentJson {
    public HashMap<String, Map<String, Workspace>> workspaces = new HashMap<String, Map<String,Workspace>>();
    public Boolean auto_generated_account = false;
    public Boolean disable_account_creation = true;
    public LinkedList<Workspace> recent_workspaces = new LinkedList<Workspace>();

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
        this.recent_workspaces.add(0, workspace);
        HashSet<String> seen = new HashSet<String>();
        LinkedList<Workspace> unique = new LinkedList<Workspace>();
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
            Flog.warn(e);
        }
    }

    public static File getFile() {
        return new File(FilenameUtils.concat(Shared.baseDir, "persistent.json"));
    }

    public static PersistentJson getInstance() {
        String s;
        String defaultJSON = "{}";
        try {
            s = FileUtils.readFileToString(getFile(), "UTF-8");
        } catch (Exception e) {
            Flog.warn(e);
            s = defaultJSON;
        }
        PersistentJson pj;
        try {
            pj = new Gson().fromJson(s, (Type) PersistentJson.class);
        } catch (com.google.gson.JsonSyntaxException e) {
            Flog.warn("Bad JSON in persistent json");
            pj = new Gson().fromJson(defaultJSON, (Type) PersistentJson.class);
        }
        if (pj == null) {
            pj = new Gson().fromJson(defaultJSON, (Type) PersistentJson.class);
        }
        return pj;
    }
}
