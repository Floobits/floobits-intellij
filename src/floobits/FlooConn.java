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

import floobits.FlooHandler;

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

public class FlooConn extends Thread {
    private static Logger Log = Logger.getInstance(Listener.class);

    protected Writer out;
    protected FlooHandler handler;

    public FlooConn(FlooHandler handler) {
        this.handler = handler;
    }

    private void handle (String line) {
        Log.info(String.format("response: %s", line));
        JsonObject obj = (JsonObject)new JsonParser().parse(line);
        JsonElement name = obj.get("name");
        this.handler.on_data(name.getAsString(), obj);
    };

    public void write (Serializable obj) {
        String data = new Gson().toJson(obj);
        Log.debug(data);
        try {
            this.out.write(data + "\n");
            this.out.flush();
        } catch (Exception e) {
            Log.error(e);
        }
    }

    public void run () {

        int port = 3448; // default https port
        String host = "floobits.com";
        // String url = String.format("%s/%s/%s", host, owner, workspace);

//        TrustManager[] trustAll = new javax.net.ssl.TrustManager[]{
//            new javax.net.ssl.X509TrustManager(){
//                public java.security.cert.X509Certificate[] getAcceptedIssuers(){
//                    return null;
//                }
//                public void checkClientTrusted(java.security.cert.X509Certificate[] certs,String authType){}
//                public void checkServerTrusted(java.security.cert.X509Certificate[] certs,String authType){}
//            }
//        };
//
        try {
//
//            // TODO: verify ssl cert
//            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
//            sc.init(null, trustAll, new java.security.SecureRandom());

            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
//            SSLSocketFactory factory = (SSLSocketFactory) sc.getSocketFactory();
            SSLSocket socket = (SSLSocket) factory.createSocket(host, port);

            this.out = new OutputStreamWriter(socket.getOutputStream());

            // read response
            BufferedReader in = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));

            String line;
            this.handler.on_ready();

            while (true) {
                try {
                    line = in.readLine();
                    if (line == null) {
                        Log.warn("socket died");
                        break;
                    }
                    this.handle(line);
                } catch (IOException e) {
                    Log.error(e);
                    break;
                }
            }
            this.out.close();
            in.close();
            socket.close();
        } catch (Exception e) {
            Log.error(e);
        }
    }
}
