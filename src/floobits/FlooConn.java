package floobits;

import java.util.*;
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

    public RoomInfo(Map<String, String> settings) {
        this.username = settings.get("username");
        this.room = settings.get("workspace");
        this.room_owner = settings.get("room_owner");
        this.secret = settings.get("secret");
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
    public Map<String, String> settings = new HashMap<String, String>();

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