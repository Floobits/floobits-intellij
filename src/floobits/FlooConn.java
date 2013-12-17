package floobits;

import java.io.*;
import java.security.*;
import javax.net.ssl.*;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.application.ApplicationManager;


public class FlooConn extends Thread {
    protected Writer out;
    protected BufferedReader in;
    protected SSLSocket socket;
    protected FlooHandler handler;

    private Integer MAX_RETRIES = 20;
    private Integer INITIAL_RECONNECT_DELAY = 500;
    protected Integer retries = MAX_RETRIES;
    protected Integer delay = INITIAL_RECONNECT_DELAY;

    public FlooConn(FlooHandler handler) {
        this.handler = handler;
    }

    public void write (Serializable obj) {
        String data = new Gson().toJson(obj);
        try {
            this.out.write(data + "\n");
            this.out.flush();
        } catch (Exception e) {
            if (retries > -1) Flog.error(e);
        }
    }

    public void run () {
        connect();
    }

    public void shut_down() {
        retries = -1;
        cleanup();
    }

    protected void cleanup() {
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null)  {
                socket.shutdownOutput();
                socket.shutdownOutput();
                socket.close();
            }
       } catch (Exception ignored) {}
    }

    protected void handle (String line) {
        JsonObject obj = (JsonObject)new JsonParser().parse(line);
        JsonElement name = obj.get("name");
        if (name == null) {
            Flog.warn("No name for request, ignoring");
            return;
        }
        String name1 = name.getAsString();
        try {
            this.handler.on_data(name1, obj);
        } catch (Exception e) {
            Flog.error(e);
            if (name1.equals("room_info")) {
                shut_down();
            }
        }
    }

    protected void reconnect() {
        cleanup();
        retries -= 1;
        if (retries <= 0) {
            Flog.error("I give up connecting.");
            return;
        }
        delay = Math.min(10000, Math.round((float)1.5 * delay));
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Flog.error(e);
        }
        connect();
    }

    protected void connect() {
        FlooUrl flooUrl = handler.getUrl();
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();

        try {
            socket = (SSLSocket) factory.createSocket(flooUrl.host, flooUrl.port);
        } catch (IOException e) {
            reconnect();
        }

        delay = INITIAL_RECONNECT_DELAY;
        retries = MAX_RETRIES;

        try {
            out = new OutputStreamWriter(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line;
            this.handler.on_connect();

            while (true) {
                try {
                    line = in.readLine();
                    if (line == null) {
                        if (retries != -1) Flog.warn("socket died");
                        break;
                    }
                    this.handle(line);
                } catch (IOException e) {
                    if (retries != -1) Flog.warn(e);
                    break;
                }
            }
        } catch (IOException e) {
            reconnect();
        } catch (Exception e) {
            if (retries != -1) Flog.error(e);
            reconnect();
        }
    }
}
