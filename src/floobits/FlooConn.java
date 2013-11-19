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
    private String version = "1.0";
    private String[] supported_encodings =  {"utf8"};

    public RoomInfo(String username, String room, String owner) {
        this.username = username;
        this.room = room;
        this.room_owner = owner;
    }
}


public class FlooConn {
    private static Logger Log = Logger.getInstance(Listener.class);
    public FlooConn(String owner, String workspace) {

        int port = 443; // default https port
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


        try {

            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
            sc.init(null, trustAll, new java.security.SecureRandom());

//            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
//            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
//
//            SSLSocket socket = (SSLSocket) factory.createSocket(host, port);


            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            SSLSocketFactory factory = (SSLSocketFactory) sc.getSocketFactory();
            SSLSocket socket = (SSLSocket) factory.createSocket(host, port);

            Writer out = new OutputStreamWriter(socket.getOutputStream());
            Gson gson = new Gson();
            String data = gson.toJson(new RoomInfo("asdf", owner, workspace));

            out.write(data);
            out.write("/n");
            out.flush();

            // read response
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            String line;

            while (true){
                try {
                    line = in.readLine();
                    Log.info(line);
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