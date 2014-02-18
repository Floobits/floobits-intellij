package floobits.common;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import floobits.handlers.BaseHandler;
import floobits.utilities.Flog;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.SocketTimeoutException;

public class FlooConn extends Thread {
    private class Ping implements Serializable { public String name = "ping"; }
    protected Writer out;
    protected BufferedReader in;
    protected SSLSocket socket;
    protected BaseHandler handler;
    private Timeout timeout;

    private Integer MAX_RETRIES = 20;
    private Integer INITIAL_RECONNECT_DELAY = 500;
    protected Integer retries = MAX_RETRIES;
    protected Integer delay = INITIAL_RECONNECT_DELAY;

    public FlooConn(BaseHandler handler) {
        this.handler = handler;
    }

    public synchronized void write (Serializable obj) {
        String data = new Gson().toJson(obj);
        try {
            out.write(data + "\n");
            out.flush();
        } catch (Exception e) {
            if (retries > -1) Flog.warn(e);
        }
    }

    public void run () {
        connect();
    }

    public void shutdown() {
        retries = -1;
        handler = null;
        cleanUp();
    }

    protected void cleanUp() {

        if (out != null) {
            try {
                out.close();
            } catch (Exception ignored) {}
            out = null;
        }

        if (in != null) {
            try {
                in.close();
            } catch (Exception ignored) {}
            in = null;
        }

        if (socket != null)  {
            try {
                socket.close();
            } catch (Exception ignored) {}
            socket = null;
        }

        if (timeout != null) {
            try {
                timeout.cancel();
            } catch (Exception ignored) {}
            timeout = null;
        }
    }

    private void setTimeout() {
        write(new Ping());
        if (timeout != null) {
            timeout.cancel();
        }
        timeout = handler.context.setTimeout(3000, new Runnable() {
             @Override
             public void run() {
                timeout = null;
                reconnect();
             }
        });
    }

    protected void handle (String line) {
        JsonObject obj = (JsonObject)new JsonParser().parse(line);
        JsonElement name = obj.get("name");
        if (name == null) {
            Flog.warn("No name for receive, ignoring");
            return;
        }
        String requestName = name.getAsString();
        if (requestName.equals("pong")) {
            if (timeout != null) timeout.cancel();

            timeout = handler.context.setTimeout(1000, new Runnable() {
                @Override
                public void run() {
                    setTimeout();
                }
            });
            return;
        }
        handler.on_data(requestName, obj);
    }

    protected void reconnect() {
        Flog.info("reconnecting");
        cleanUp();
        retries -= 1;
        if (retries <= 0) {
            Flog.warn("I give up connecting.");
            try {
                handler.context.shutdown();
            } catch (Exception ignore) {}
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
        SSLContext sslContext = Utils.createSSLContext();
        if (sslContext == null) {
            Utils.error_message("I can't do SSL.", handler.getProject());
            return;
        }
        try{
            socket = (SSLSocket) sslContext.getSocketFactory().createSocket(flooUrl.host, flooUrl.port);
            Integer SOCKET_TIMEOUT = 0;
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
            handler.on_connect();
            timeout = handler.context.setTimeout(10000, new Runnable() {
                @Override
                public void run() {
                    timeout = null;
                    setTimeout();
                }
            });
            String line;
            while (true) {
                try {
                    line = in.readLine();
                    if (line == null) {
                        if (retries != -1) Flog.warn("socket died");
                        break;
                    }
                    retries = MAX_RETRIES;
                    delay = INITIAL_RECONNECT_DELAY;
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
