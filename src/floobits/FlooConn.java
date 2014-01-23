package floobits;

import java.io.*;
import java.net.SocketTimeoutException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import javax.net.ssl.*;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static floobits.Utils.createSSLContext;

public class FlooConn extends Thread {
    protected Writer out;
    protected BufferedReader in;
    protected SSLSocket socket;
    protected FlooHandler handler;
    Boolean connected = false;

    private Integer MAX_RETRIES = 20;
    private Integer SOCKET_TIMEOUT = 0; // Inifinity right now, which is default.
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
            if (retries > -1) Flog.warn(e);
        }
    }

    public void run () {
        connect();
    }

    public Boolean shutDown() {
        if (!connected) {
            // Not connectedd.
            return false;
        }
        retries = -1;
        cleanUp();
        return true;
    }

    protected void cleanUp() {
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
        connected = false;
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
            Flog.warn(e);
            if (name1.equals("room_info")) {
                shutDown();
            }
        }
    }

    protected void reconnect() {
        Flog.info("reconnecting");
        if (!connected) {
            Flog.info("Aborting reconnect, we do not want to be connected.");
            return;
        }
        cleanUp();
        retries -= 1;
        if (retries <= 0) {
            Flog.warn("I give up connecting.");
            FloobitsPlugin.flooHandler.shut_down();
            return;
        }
        delay = Math.min(10000, Math.round((float)1.5 * delay));
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Flog.warn(e);
        }
        connect();
    }

    protected void connect() {
        FlooUrl flooUrl = handler.getUrl();
        SSLContext sslContext = createSSLContext();
        if (sslContext == null) {
            Flog.throwAHorribleBlinkingErrorAtTheUser("I can't do SSL.");
            return;
        }
        try{
            socket = (SSLSocket) sslContext.getSocketFactory().createSocket(flooUrl.host, flooUrl.port);
            socket.setSoTimeout(SOCKET_TIMEOUT);
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);
        } catch (IOException e) {
            Flog.warn("Error connecting %s", e);
            reconnect();
            return;
        }
        try {
            out = new OutputStreamWriter(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line;
            this.handler.on_connect();
            connected = true;
            retries = MAX_RETRIES;
            delay = INITIAL_RECONNECT_DELAY;

            while (true) {
                try {
                    line = in.readLine();
                    if (line == null) {
                        if (retries != -1) Flog.warn("socket died");
                        break;
                    }
                    this.handle(line);
                } catch (SocketTimeoutException e) {
                    Flog.info("Caught timeout on socket. %s", socket.isClosed());
                    if (socket.isClosed()) {
                        reconnect();
                        return;
                    }
                } catch (IOException e) {
                    if (retries != -1) Flog.warn(e);
                    break;
                }
            }
        } catch (Exception e) {
            if (retries != -1) Flog.warn(e);
        } finally {
            reconnect();
        }
    }
}
