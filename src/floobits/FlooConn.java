package floobits;

import java.util.*;
import java.io.*;
import java.security.*;
import javax.net.ssl.*;
import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;
import com.google.gson.JsonParser;
import com.google.gson.*;
import java.util.Map.Entry;
import java.lang.reflect.Method;

class RoomInfo {
    private String username = "";
    private String room = "";
    private String room_owner;
    private String platform = "???";
    private String version = "0.03";
    private String[] supported_encodings =  {"utf8"};
    private String secret = "";
    private String client = "untellij";

    public RoomInfo(Map<String, String> settings) {
        this.username = settings.get("username");
        this.room = settings.get("workspace");
        this.room_owner = settings.get("room_owner");
        this.secret = settings.get("secret");
    }
}

class Buf implements Serializable {
    private Integer id;
    private String md5;
    private String path;
    private String encoding;
};

class User implements Serializable {
    public String[] perms;
    public String client;
    public String platform;
    public Integer user_id;
    public String username;
    public String version;
};

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
};

class RoomInfoResponse implements Serializable{
    public String[] anon_perms;
    public Integer max_size;
    public String name;
    public String owner;
    public String[] perms;
    public String room_name;
    public Boolean secret;
    public HashMap<Integer, User> users;
    public HashMap<Integer, Buf> bufs;
    public Tree tree;

//    private ??? temp_data;
//    private ??? terms;
};


class Path {
    public static String combine(String... paths)
    {
        File file = new File(paths[0]);

        for (int i = 1; i < paths.length ; i++) {
            file = new File(file, paths[i]);
        }

        return file.getPath();
    }
};

public class FlooConn extends Thread{
    private static Logger Log = Logger.getInstance(Listener.class);
    public String[] perms;
    public Map<String, String> settings = new HashMap<String, String>();
    public Map<Integer, User> users = new HashMap<Integer, User>();
    public Tree tree;

    public FlooConn(String room_owner, String workspace) {
        this.settings.put("room_owner", room_owner);
        this.settings.put("workspace", workspace);
    }

    private void read_floorc() {
        String userHome = System.getProperty( "user.home" );
        String floorc_path = Path.combine(userHome, ".floorc");
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(floorc_path));
            String line = br.readLine();

            while (line != null) {
                if (line.length() < 1 || line.substring(0, 1).equals("#")){
                    line = br.readLine();
                    continue;
                }
                String[] shit = line.split(" ");
                this.settings.put(shit[0], shit[1]);
                line = br.readLine();
            }
        } catch (Exception e) {
            Log.error(e);
        } finally {


        }
    }

    private void handle(String line){
        Log.info(String.format("response: %s", line));
        JsonObject obj = (JsonObject)new JsonParser().parse(line);
        JsonElement name = obj.get("name");
        String method_name;
        if (name == null) {
            method_name = "room_info";
        } else {
            method_name = name.getAsString();
        }
        try {
            Method method = this.getClass().getMethod("_on_" + method_name, new Class[]{String.class, JsonObject.class});
        } catch (NoSuchMethodException e) {
            Log.error(e);
            return;
        }
        if (method != null) {
            Object arglist[] = new Object[2];
            arglist[0] = line;
            arglist[1] = obj;
            try {
                method.invoke(this, arglist);
            } catch (Exception e) {
                Log.error(e);
            }
        }
    };


    private void on_room_info(String line, JsonObject obj) {
        RoomInfoResponse ri = new Gson().fromJson(line, RoomInfoResponse.class);
        JsonObject tree_obj =  obj.getAsJsonObject("tree");
        ri.tree = new Tree(tree_obj);
        this.users = ri.users;
        this.perms = ri.perms;
        this.tree = ri.tree;
    };

    public void run() {

        this.read_floorc();


        int port = 3448; // default https port
        String host = "floobits.com";
//        String url = String.format("%s/%s/%s", host, owner, workspace);

        TrustManager[] trustAll = new javax.net.ssl.TrustManager[]{
            new javax.net.ssl.X509TrustManager(){
                public java.security.cert.X509Certificate[] getAcceptedIssuers(){
                    return null;
                }
                public void checkClientTrusted(java.security.cert.X509Certificate[] certs,String authType){}
                public void checkServerTrusted(java.security.cert.X509Certificate[] certs,String authType){}
            }
        };

        Log.info(String.format("%s", this.settings));

        try {

            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
            sc.init(null, trustAll, new java.security.SecureRandom());

            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
//            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocketFactory factory = (SSLSocketFactory) sc.getSocketFactory();
            SSLSocket socket = (SSLSocket) factory.createSocket(host, port);

            Writer out = new OutputStreamWriter(socket.getOutputStream());
            String data = new Gson().toJson(new RoomInfo(this.settings));

            out.write(data + "\n");
            out.flush();

            // read response
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            String line;

            while (true){
                try {
                    line = in.readLine();
                    if (line == null) {
                        Log.warn("socket died");
                        break;
                    }
                    this.handle(line);
                } catch (IOException e) {
                    break;
                }
            }
            out.close();
            in.close();
            socket.close();
        }catch (Exception e) {
            Log.error(e);
        }
    }
}