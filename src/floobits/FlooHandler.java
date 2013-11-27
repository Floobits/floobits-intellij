package floobits;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

import com.intellij.openapi.diagnostic.Logger;

import com.google.gson.Gson;
import com.google.gson.*;
import com.google.gson.JsonParser;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import dmp.diff_match_patch;
import dmp.diff_match_patch.Patch;

import floobits.FlooUrl;
import floobits.Settings;
import floobits.PersistentJson;
import floobits.Buf;

class FlooAuth implements Serializable {
    public String username;
    public String api_key;
    public String secret;

    public String room;
    public String room_owner;
    public String platform = "???";
    public String version = "0.03";
    public String[] supported_encodings = {"utf8"};
    public String client = "untellij";

    public FlooAuth(Settings settings, String owner, String workspace) {
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
    public Tree(JsonObject obj) {
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

//    private ??? temp_data;
//    private ??? terms;
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
}

class PatchResponse implements Serializable {
    public Integer user_id;
    public String md5_after;
    public String md5_before;
    public Integer id;
    public String patch;

    // Deprecated
    public String path;
    public String username;
}

class FlooHandler {
    private static Logger Log = Logger.getInstance(FlooHandler.class);
    protected diff_match_patch dmp;
    public String[] perms;
    public Map<Integer, User> users = new HashMap<Integer, User>();
    public HashMap<Integer, Buf> bufs = new HashMap<Integer, Buf>();
    public Tree tree;
    public String colabDir;

    public FlooUrl url;
    public FlooConn conn;

    public FlooHandler(FlooUrl f) {
        this.url = f;
        this.dmp = new diff_match_patch();

        PersistentJson p = new PersistentJson();
        try {
            this.colabDir = p.workspaces.get(f.owner).get(f.workspace).path;
        } catch (Exception e) {
            Log.error(e);
            // TODO: colab dir isn't in persistent.json. ask user for dir to save to
            return;
        }
        this.conn = new FlooConn(f.host, this);
        this.conn.start();
    }

    public void on_ready () {
        this.conn.write(new FlooAuth(new Settings(), this.url.owner, this.url.workspace));
    }

    public void on_data (String name, JsonObject obj) {
        String method_name = "_on_" + name;
        Method method;
        Class c;
        try {
            method = this.getClass().getDeclaredMethod(method_name, new Class[]{JsonObject.class});
            Object arglist[] = new Object[1];
            arglist[0] = obj;
            try {
                method.invoke(this, arglist);
            } catch (Exception e) {
                Log.error(e);
            }
        } catch (NoSuchMethodException e) {
            Log.error(e);
            return;
        }
    }

    @SuppressWarnings("unused")
    protected void _on_room_info (JsonObject obj) {
        RoomInfoResponse ri = new Gson().fromJson(obj, RoomInfoResponse.class);
        this.tree = new Tree(obj.getAsJsonObject("tree"));
        this.users = ri.users;
        this.perms = ri.perms;
        Buf buf;
        for (Map.Entry entry : ri.bufs.entrySet()) {
            Integer buf_id = (Integer) entry.getKey();
            RiBuf b = (RiBuf) entry.getValue();
            buf = Buf.createBuf(b.path, b.id, Encoding.from(b.encoding), b.md5);
            if (buf.buf == null) {
                this.send_get_buf(buf_id);
            }
            this.bufs.put(buf_id, buf);
        }
    }

    @SuppressWarnings("unused")
    protected void _on_get_buf (JsonObject obj) throws IOException {
        // TODO: be nice about this and update the existing view
        GetBufResponse gb = new Gson().fromJson(obj, GetBufResponse.class);
        Log.info(String.format("Got buf %s %s", gb.id, gb.path));
        String absPath = FilenameUtils.concat(this.colabDir, gb.path);
        File f = new File(absPath);
        File parent = new File(f.getParent());
        parent.mkdirs();
        FileUtils.write(f, gb.buf, "UTF-8");
    }

    @SuppressWarnings("unused")
    protected void _on_patch (JsonObject obj) throws IOException {
        PatchResponse pr = new Gson().fromJson(obj, PatchResponse.class);
        File absPath = new File(FilenameUtils.concat(this.colabDir, pr.path));
        String s = FileUtils.readFileToString(absPath, "UTF-8");

        Log.info(String.format("Got _on_patch"));

        String md5Before = DigestUtils.md5Hex(s);
        if (!md5Before.equals(pr.md5_before)) {
            Log.info(String.format("MD5 before mismatch (ours %s remote %s). Sending get_buf.", md5Before, pr.md5_before));
            this.send_get_buf(pr.id);
            return;
        }

        LinkedList<Patch> patches;
        patches = (LinkedList) dmp.patch_fromText(pr.patch);
        Object[] results = dmp.patch_apply(patches, s);
        String text = (String) results[0];
        boolean[] boolArray = (boolean[]) results[1];

        boolean cleanPatch = true;
        for (boolean clean : boolArray) {
            if (clean == false) {
                cleanPatch = false;
                break;
            }
        }

        if (!cleanPatch) {
            Log.info("Patch not clean. Sending get_buf.");
            this.send_get_buf(pr.id);
            return;
        }

        String md5After = DigestUtils.md5Hex(text);
        if (!md5After.equals(pr.md5_after)) {
            Log.info(String.format("MD5 after mismatch (ours %s remote %s). Sending get_buf.", md5After, pr.md5_after));
            this.send_get_buf(pr.id);
            return;
        }

        Log.info(String.format("Writing patch to buf %s %s", pr.id, pr.path));
        FileUtils.write(absPath, text, "UTF-8");
    }

    @SuppressWarnings("unused")
    protected void _on_highlight (JsonObject obj) {
        Log.info(String.format("Got _on_highlight"));
    }

    @SuppressWarnings("unused")
    protected void _on_saved (JsonObject obj) {
        Log.info(String.format("Got _on_saved"));
    }

    @SuppressWarnings("unused")
    protected void _on_join (JsonObject obj) {
        Log.info(String.format("Got _on_join"));
    }

    @SuppressWarnings("unused")
    protected void _on_part (JsonObject obj) {
        Log.info(String.format("Got _on_part"));
    }

    @SuppressWarnings("unused")
    protected void _on_disconnect (JsonObject obj) {
        String reason = obj.get("reason").getAsString();
        Log.warn(String.format("Disconnected: %s", reason));
    }

    public void send_get_buf (Integer buf_id) {
        this.conn.write(new GetBufRequest(buf_id));
    }

    public void send_patch () {
        
    }
}
