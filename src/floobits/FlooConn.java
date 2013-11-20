package floobits;

import java.net.*;
import java.io.*;
import java.security.*;
import javax.net.ssl.*;
import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;


class RoomInfo {
    private String username = "";
    private String room = "";
    private String room_owner;
    private String platform = "???";
    private String version = "0.03";
    private String[] supported_encodings =  {"utf8"};
    private String secret = "";
    private String client = "untellij";

    public RoomInfo(String username, String secret, String room, String room_owner) {
        this.username = username;
        this.room = room;
        this.room_owner = room_owner;
        this.secret = secret;
    }
}
class Path {
    public static String combine(String... paths)
    {
        File file = new File(paths[0]);

        for (int i = 1; i < paths.length ; i++) {
            file = new File(file, paths[i]);
        }

        return file.getPath();
    }
}

public class FlooConn extends Thread{
    private static Logger Log = Logger.getInstance(Listener.class);
    private String owner;
    private String workspace;
    private String username;
    private String secret;
    private String api_key;
    public FlooConn(String owner, String workspace) {
        this.owner = owner;
        this.workspace = workspace;
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
                String key = shit[0];
                String val = shit[1];
                if (key.equals("username")) {
                    this.username = val;
                } else if (key.equals("api_key")) {
                    this.api_key = val;
                } else if (key.equals("secret")) {
                    this.secret = val;
                }
                Log.info(String.format("%s: %s", key, val));
                line = br.readLine();
            }
        } catch (Exception e) {
            Log.error(e);
        } finally {


        }
    }

    public void run() {

        this.read_floorc();


        int port = 3448; // default https port
        String host = "staging.floobits.com";
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


        try {

            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
            sc.init(null, trustAll, new java.security.SecureRandom());

            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
//            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocketFactory factory = (SSLSocketFactory) sc.getSocketFactory();
            SSLSocket socket = (SSLSocket) factory.createSocket(host, port);

            Writer out = new OutputStreamWriter(socket.getOutputStream());
            String data = new Gson().toJson(new RoomInfo(this.username, this.secret, workspace, owner));

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
                    Log.info(String.format("response: %s", line));
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