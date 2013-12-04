package floobits;

import java.io.*;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.*;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.roots.ContentIterator;
import org.apache.commons.io.FilenameUtils;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import org.apache.commons.codec.digest.DigestUtils;
import dmp.diff_match_patch;
import dmp.diff_match_patch.Patch;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.codec.digest.DigestUtils;
// NOTES:
//  TODO: check LocalFileSystem.getInstance().findFileByIoFile() or maybe FileDocumentManager.getCachedDocument()
// LocalFileSystem.getInstance().findFileByPathIfCached();
// guessCurrentProject1
// ProjectRootManager.getInstance(project).getFileIndex().iterateContent()
// open new project : http://devnet.jetbrains.com/message/5492018#5492018
// list all open projects ProjectManager.getInstance().getOpenProjects()
// open arbitrary file : FileEditorManager.openFile
// create a new project: http://devnet.jetbrains.com/message/5106735#5106735

class FlooAuth implements Serializable {
    public String username;
    public String api_key;
    public String secret;

    public String room;
    public String room_owner;
    public String client = "IntelliJ";
    public String platform = System.getProperty("os.name");
    public String version = "0.10";
    public String[] supported_encodings = { "utf8", "base64" };

    public FlooAuth (Settings settings, String owner, String workspace) {
        this.username = settings.get("username");
        this.api_key = settings.get("api_key");
        this.room = workspace;
        this.room_owner = owner;
        this.secret = settings.get("secret");
    }
}

class RiBuf implements Serializable {
    public Integer id;
    public String md5;
    public String path;
    public String encoding;
}

class User implements Serializable {
    public String[] perms;
    public String client;
    public String platform;
    public Integer user_id;
    public String username;
    public String version;
}

class Tree implements Serializable {
    public HashMap<String, Integer> bufs;
    public HashMap<String, Tree> folders;
    public Tree (JsonObject obj) {
        this.bufs = new HashMap<String, Integer>();
        this.folders = new HashMap<String, Tree>();
        for (Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (value.isJsonPrimitive()) {
                this.bufs.put(key, Integer.parseInt(value.getAsString()));
            } else {
                this.folders.put(key, new Tree(value.getAsJsonObject()));
            }
        }
    }
}

class RoomInfoResponse implements Serializable {
    public String[] anon_perms;
    public Integer max_size;
    public String name;
    public String owner;
    public String[] perms;
    public String room_name;
    public Boolean secret;
    public HashMap<Integer, User> users;
    public HashMap<Integer, RiBuf> bufs;

//    public ??? temp_data;
//    public HashMap<Integer, Term> terms;
}

class GetBufRequest implements Serializable {
    public String name = "get_buf";
    public Integer id;

    public GetBufRequest (Integer buf_id) {
        this.id = buf_id;
    }
}

class GetBufResponse implements Serializable {
    public String name;
    public Integer id;
    public String path;
    public String buf;
    public String encoding;
    public String md5;
}

class FlooPatch implements Serializable {
    public String name = "patch";
    public Integer id;
    public Integer user_id;
    public String md5_after;
    public String md5_before;
    public String patch;

    // Deprecated
    public String path;
    public String username;

    private static diff_match_patch dmp = new diff_match_patch();

    public FlooPatch (String current, Buf buf) {
        // TODO: handle binary bufs
        LinkedList<Patch> patches = dmp.patch_make((String) buf.buf, current);

        this.path = buf.path;
        this.md5_before = buf.md5;
        this.md5_after = DigestUtils.md5Hex(current);
        this.id = buf.id;
        this.patch = dmp.patch_toText(patches);
        buf.set(current, this.md5_after);
    }
}

class FlooSetBuf implements Serializable {
    public String name = "set_buf";
    public Integer id;
    public String buf;
    public String md5;
    public String encoding;
    public FlooSetBuf (Buf buf) {
        this.md5 = buf.md5;
        this.id = buf.id;
        this.buf = (String) buf.buf;
        this.encoding = buf.encoding.toString();
    }
}

class FlooCreateBuf implements Serializable {
    public String name = "create_buf";
    public String buf;
    public String path;
    public String md5;
    public String encoding;
    
    public FlooCreateBuf (VirtualFile virtualFile) throws IOException {
        this.path = Utils.toProjectRelPath(virtualFile.getCanonicalPath());
        this.buf = new String(virtualFile.contentsToByteArray(), "UTF-8");
        this.md5 = DigestUtils.md5Hex(this.buf);
        this.encoding = "utf8";
    }
}

class FlooHandler {
    protected static FlooHandler flooHandler;
    protected static diff_match_patch dmp = new diff_match_patch();
    protected Project project;
    protected Boolean stomp = false;
    public String[] perms;
    public Map<Integer, User> users = new HashMap<Integer, User>();
    public HashMap<Integer, Buf> bufs = new HashMap<Integer, Buf>();
    public HashMap<String, Integer> paths_to_ids = new HashMap<String, Integer>();
    public Tree tree;

    public FlooUrl url;
    public FlooConn conn;

    public static FlooHandler getInstance() {
        return flooHandler;
    }

    public FlooHandler (FlooUrl f) {
        this.url = f;
        flooHandler = this;

        PersistentJson p = new PersistentJson();
        try {
            Shared.colabDir = p.workspaces.get(f.owner).get(f.workspace).path;
        } catch (Exception e) {
            Flog.error(e);
            // TODO: colab dir isn't in persistent.json. ask user for dir to save to
            return;
        }
        this.conn = new FlooConn(f.host, this);
        this.conn.start();
    }

    public FlooHandler (Project p) {
        this.project = p;
        flooHandler = this;
        this.stomp = true;
        String project_path = p.getBasePath();
        Shared.colabDir = project_path;
        PersistentJson persistentJson = new PersistentJson();

        for (Entry<String, Map<String, Workspace>> i : persistentJson.workspaces.entrySet()) {
            Map<String, Workspace> workspaces = i.getValue();
            for (Entry<String, Workspace> j : workspaces.entrySet()) {
                Workspace w = j.getValue();
                if (FilenameUtils.equalsNormalized(w.path, project_path)) {
                    try {
                        this.url = new FlooUrl(w.url);
                    } catch (MalformedURLException e) {
                        Flog.error(e);
                        break;
                    }
                    Shared.colabDir = w.path;
                    API.getWorkspace(url.owner, url.workspace);
                    this.conn = new FlooConn(this.url.host, this);
                    this.conn.start();
                    return;
                }
            }
        }
        Settings settings = new Settings();
        String owner = settings.get("username");
        String name = new File(project_path).getName();
        Integer code = API.createWorkspace(owner, name);
        switch (code) {
            case 409:
                Flog.warn("Already exists");
            case 201:
                this.url = new FlooUrl(Shared.defaultHost, owner, name, -1, true);
                this.conn = new FlooConn(this.url.host, this);
                this.conn.start();
                break;
            case 400:
                Flog.warn("Invalid name");
                break;
            case 402:
                Flog.warn("Details in body");
               break;
            default:
                Flog.warn("Unknown error");
        }
    }

    public void on_ready () {
        this.conn.write(new FlooAuth(new Settings(), this.url.owner, this.url.workspace));
    }

    public void on_data (String name, JsonObject obj) {
        String method_name = "_on_" + name;
        Method method;
        try {
            method = this.getClass().getDeclaredMethod(method_name, new Class[]{JsonObject.class});
            Object arglist[] = new Object[1];
            arglist[0] = obj;
            try {
                Flog.log("Calling %s", method_name);
                method.invoke(this, arglist);
            } catch (Exception e) {
                Flog.error(e);
            }
        } catch (NoSuchMethodException e) {
            Flog.error(e);
            return;
        }
    }

    protected Buf get_buf_by_path(String path) {
        Integer id = this.paths_to_ids.get(path);
        if (id == null) {
            return null;
        }
        return this.bufs.get(id);
    }

    protected void _on_room_info (JsonObject obj) {
        RoomInfoResponse ri = new Gson().fromJson(obj, RoomInfoResponse.class);
        this.tree = new Tree(obj.getAsJsonObject("tree"));
        this.users = ri.users;
        this.perms = ri.perms;
        LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
        Buf buf;
        for (Map.Entry entry : ri.bufs.entrySet()) {
            Integer buf_id = (Integer) entry.getKey();
            RiBuf b = (RiBuf) entry.getValue();
            buf = Buf.createBuf(b.path, b.id, Encoding.from(b.encoding), b.md5);
            this.bufs.put(buf_id, buf);
            this.paths_to_ids.put(b.path, b.id);
            String path = Utils.absPath(b.path);
            VirtualFile virtualFile = localFileSystem.findFileByPath(path);
            if (virtualFile == null || !virtualFile.exists()) {
                this.send_get_buf(buf.id);
                continue;
            }
            String contents = virtualFile.toString();
            String md5 = DigestUtils.md5Hex(contents);
            if (!md5.equals(buf.md5)) {
                if (this.stomp) {
                    this.send_set_buf(buf);
                } else {
                    this.send_get_buf(buf.id);
                }
                continue;
            }
            buf.buf = contents;
        }
        if (!this.stomp) {
            return;
        }

        ProjectRootManager.getInstance(project).getFileIndex().iterateContent(new ContentIterator() {

            public boolean processFile(final VirtualFile vfile) {
                boolean we_care = vfile.exists() && vfile.isInLocalFileSystem() &&
                        !(vfile.isDirectory() && vfile.isSpecialFile() && vfile.isSymLink());

                if (!we_care) {
                    return true;
                }
                String path = vfile.getPath();
                if (!Utils.isShared(path)) {
                    Flog.info("Thing isn't shared: %s", path);
                    return true;
                }
                String rel_path = Utils.toProjectRelPath(path);
                Buf b = flooHandler.get_buf_by_path(rel_path);
                if (b == null) {
                    flooHandler.send_create_buf(vfile);
                }
                return true;
            }

        });
    }

    public void send_create_buf(VirtualFile virtualFile) {
        FlooCreateBuf flooCreateBuf;
        try {
            flooCreateBuf = new FlooCreateBuf(virtualFile);
        } catch (IOException e) {
            Flog.error(e);
            return;
        }
        this.conn.write(flooCreateBuf);
    }

    protected void _on_get_buf (JsonObject obj) {
        // TODO: be nice about this and update the existing view
        Gson gson = new Gson();
        GetBufResponse res = gson.fromJson(obj, GetBufResponse.class);

        Buf b = this.bufs.get(res.id);
        b.set(res.buf, res.md5);
    }

    protected void _on_patch (JsonObject obj) throws IOException {
        FlooPatch res = new Gson().fromJson(obj, FlooPatch.class);
        Buf b = this.bufs.get(res.id);
        if (b.buf == null) {
            Flog.warn("no buffer");
            this.send_get_buf(res.id);
            return;
        }

        if (res.patch.length() == 0) {
            Flog.warn("wtf? no patches to apply. server is being stupid");
            return;
        }

        Flog.info("Got _on_patch");

        if (!b.md5.equals(res.md5_before)) {
            Flog.info("MD5 before mismatch (ours %s remote %s). Sending get_buf.", b.md5, res.md5_before);
            this.send_get_buf(res.id);
            return;
        }

        LinkedList<Patch> patches;
        patches = (LinkedList) dmp.patch_fromText(res.patch);
        Object[] results = dmp.patch_apply(patches, (String) b.buf);
        final String text = (String) results[0];
        boolean[] boolArray = (boolean[]) results[1];

        boolean cleanPatch = true;
        for (boolean clean : boolArray) {
            if (!clean) {
                cleanPatch = false;
                break;
            }
        }

        if (!cleanPatch) {
            Flog.info("Patch not clean. Sending get_buf.");
            this.send_get_buf(res.id);
            return;
        }

        String md5After = DigestUtils.md5Hex(text);
        if (!md5After.equals(res.md5_after)) {
            Flog.info("MD5 after mismatch (ours %s remote %s). Sending get_buf.", md5After, res.md5_after);
            this.send_get_buf(res.id);
            return;
        }
        Flog.log("Patched %s", res.path);

        b.set(text, res.md5_after);
    }

    protected void _on_highlight (JsonObject obj) {
        Flog.info("Got _on_highlight");
    }

    protected void _on_saved (JsonObject obj) {
        Flog.info("Got _on_saved");
    }

    protected void _on_join (JsonObject obj) {
        User u = new Gson().fromJson(obj, User.class);
        Flog.info("%s joined the workspace", u.username);
        this.users.put(u.user_id, u);
    }

    protected void _on_part (JsonObject obj) {
        Integer userId = obj.get("user_id").getAsInt();
        User u = users.get(userId);
        Flog.info("%s left the workspace", u.username);
        this.users.remove(userId);
    }

    protected void _on_disconnect (JsonObject obj) {
        String reason = obj.get("reason").getAsString();
        Flog.warn("Disconnected: %s", reason);
    }

    public void send_get_buf (Integer buf_id) {
        this.conn.write(new GetBufRequest(buf_id));
    }

    public void send_patch (String current, Buf buf) {
        FlooPatch req = new FlooPatch(current, buf);
        
        this.conn.write(req);
    }

    public void send_set_buf (Buf b) {
        this.conn.write(new FlooSetBuf(b));
    }

    public void un_change (String path, String current) {
        String relPath = Utils.toProjectRelPath(path);

        Integer id = this.paths_to_ids.get(relPath);
        if (id == null) {
            Flog.info("no buf for id %s", id);
            return;
        }
        Buf buf = bufs.get(id);
        if (buf.buf == null) {
            Flog.info("buf isn't populated yet");
            return;
        }
        this.send_patch(current, buf);
    }

    @SuppressWarnings("unused")
    public void testHandlers () throws IOException {
        JsonObject obj = new JsonObject();
        this._on_room_info(obj);
        this._on_get_buf(obj);
        this._on_patch(obj);
        this._on_highlight(obj);
        this._on_saved(obj);
        this._on_join(obj);
        this._on_part(obj);
        this._on_disconnect(obj);
    }
}