package floobits;

import java.io.*;
import java.security.*;
import javax.net.ssl.*;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class FlooConn extends Thread {
    protected Writer out;
    protected BufferedReader in;
    protected SSLSocket socket;
    protected FlooHandler handler;

    public FlooConn(FlooHandler handler) {
        this.handler = handler;
    }

    private void handle (String line) {
        JsonObject obj = (JsonObject)new JsonParser().parse(line);
        JsonElement name = obj.get("name");
        this.handler.on_data(name.getAsString(), obj);
    };

    public void write (Serializable obj) {
        String data = new Gson().toJson(obj);
        try {
            this.out.write(data + "\n");
            this.out.flush();
        } catch (Exception e) {
            Flog.error(e);
        }
    }

    protected void cleanup() {
        try {
            if (out != null)
                out.close();
            if (in != null)
                in.close();
            if (socket != null)
                socket.close();
       } catch (Exception e) {}     
    }

    public void connect() {
        try {
            FlooUrl flooUrl = handler.getUrl();
            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            socket = (SSLSocket) factory.createSocket(flooUrl.host, flooUrl.port);
            out = new OutputStreamWriter(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line;
            this.handler.on_connect();

            while (true) {
                try {
                    line = in.readLine();
                    if (line == null) {
                        Flog.warn("socket died");
                        break;
                    }
                    this.handle(line);
                } catch (IOException e) {
                    Flog.error(e);
                    break;
                }
            }
        } catch (Exception e) {
            Flog.error(e);
        }
        cleanup();
    }
    public void run () {
        connect();
    }
}
