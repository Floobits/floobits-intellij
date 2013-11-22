package floobits;

import java.util.*;
import java.io.*;
import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;
import com.google.gson.JsonParser;
import com.google.gson.*;
import java.util.Map.Entry;
import java.lang.reflect.Method;

class FlooAuth implements Serializable {
    private String username = "";
    private String room = "";
    private String room_owner;
    private String platform = "???";
    private String version = "0.03";
    private String[] supported_encodings =  {"utf8"};
    private String secret = "";
    private String client = "untellij";

    public FlooAuth(Map<String, String> settings, String owner, String workspace) {
        this.username = settings.get("username");
        this.room = workspace;
        this.room_owner = owner;
        this.secret = settings.get("secret");
    }
}

class Buf implements Serializable {
    private Integer id;
    private String md5;
    private String path;
    private String encoding;
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
    public HashMap<Integer, Buf> bufs;
    public String tree;

//    private ??? temp_data;
//    private ??? terms;
}


class FlooHandler {
    private static Logger Log = Logger.getInstance(FlooHandler.class);
    public String[] perms;
    public Map<String, String> settings = new HashMap<String, String>();
    public Map<Integer, User> users = new HashMap<Integer, User>();
    public Tree tree;
    public String owner;
    public String workspace;

    public FlooConn conn;

    public FlooHandler(String owner, String workspace) {
        this.owner = owner;
        this.workspace = workspace;
        this.conn = new FlooConn(this);
        this.conn.start();
        // TODO: wait for conn to be ready
        this.conn.write(new FlooAuth(this.settings, owner, workspace));
    }

    public void on_data(String name, JsonObject obj) {
        String method_name = "_on_" + name;
        Method method;

        try {
            method = this.getClass().getDeclaredMethod(method_name, new Class[]{String.class, JsonObject.class});
        } catch (NoSuchMethodException e) {
            Log.error(e);
            return;
        }
        Object arglist[] = new Object[2];
        arglist[0] = name;
        arglist[1] = obj;
        try {
            method.invoke(this, arglist);
        } catch (Exception e) {
            Log.error(e);
        }
    }

    public void on_room_info (String name, JsonObject obj) {
        RoomInfoResponse ri = new Gson().fromJson(obj, RoomInfoResponse.class);
        this.tree = new Tree(obj.getAsJsonObject("tree"));
        this.users = ri.users;
        this.perms = ri.perms;
    };
}
