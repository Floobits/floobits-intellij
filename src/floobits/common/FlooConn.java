package floobits.common;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import floobits.common.handlers.BaseHandler;
import floobits.utilities.Flog;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.SocketTimeoutException;
import java.util.concurrent.LinkedBlockingDeque;

public class FlooConn {
    // TODO: kill client pings. just check that we got a ping from server every n seconds
    private class Ping implements Serializable { public String name = "ping"; }
    protected Writer out;
    protected BufferedReader in;
    protected SSLSocket socket;
    protected volatile BaseHandler handler;
    private volatile Timeout timeout;
    protected LinkedBlockingDeque<String> outQueue = new LinkedBlockingDeque<String>();
    protected Thread writeThread;
    protected Thread readThread;

    private Integer MAX_RETRIES = 20;
    private Integer INITIAL_RECONNECT_DELAY = 500;
    protected volatile Integer retries = MAX_RETRIES;
    protected Integer delay = INITIAL_RECONNECT_DELAY;
    protected Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    public FlooConn(final BaseHandler handler) {
        this.handler = handler;
        uncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                API.uploadCrash(handler, handler.context, throwable);
            }
        };
    }

    protected void writeLoop() {
        while(!Thread.currentThread().isInterrupted() && out != null) {
            String data;
            try {
                data = outQueue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (out == null) {
                    return;
                }
                continue;
            }
            try {
                out.write(data);
                out.flush();
            } catch (Throwable e) {
                // TODO: reconnect or something?
                if (retries > -1) {
                    Flog.warn(e);
                }
            }
        }
    }

    public void write (Serializable obj) {
        String data = new Gson().toJson(obj);
        if (out == null) {
            Flog.warn("no connection to write to");
            return;
        }
        outQueue.addLast(data + "\n");
    }

    public void start() {
        readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                connectLoop();
            }
        }, "FlooConn Read Thread");

        readThread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        readThread.start();
    }

    public void connectLoop () {
        while (retries >= 0) {
            if (handler == null) {
                Flog.log("no handler");
                break;
            }

            try {
                connect();
            } catch (NullPointerException ignored) {
            } catch (IOException e) {
                Flog.log(e.getMessage());
            }
            Flog.info("lost connection!");
            if (retries <= 0) {
                break;
            }
            retries -= 1;
            delay = Math.min(10000, Math.round((float)1.5 * delay));
            Flog.info("reconnecting after %s ms", delay);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Flog.log(e.getMessage());
            }
        }
        cleanUp();
        Flog.warn("I give up connecting.");
        try {
            handler.context.shutdown();
        } catch (Exception ignore) {}
    }

    public void shutdown() {
        retries = -1;
        try {
            readThread.interrupt();
            readThread.join(1);
        } catch (Exception ignored) {}
        cleanUp();
        handler = null;
        readThread = null;
    }

    protected void cleanUp() {
        outQueue.clear();
        if (writeThread != null) {
            writeThread.interrupt();
            try {
                writeThread.join(1);
            } catch (InterruptedException e) {
                Flog.warn(e);
            }
            writeThread = null;
        }

        if (socket != null)  {
            try {
                socket.shutdownInput();
            } catch (Exception ignored) {}
            try {
                socket.shutdownOutput();
            } catch (Exception ignored) {}
            try {
                socket.close();
            } catch (Exception ignored) {}

            socket = null;
        }

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

        if (timeout != null) {
            try {
                timeout.cancel();
            } catch (Exception ignored) {}
            timeout = null;
        }
    }

    private void setTimeout() {
        if (handler == null) {
            return;
        }
        write(new Ping());
        if (timeout != null) {
            timeout.cancel();
        }
        timeout = handler.context.setTimeout(20000, new Runnable() {
             @Override
             public void run() {
                timeout = null;
                Flog.warn("Timeout reconnecting because of timeout.");
                cleanUp();
             }
        });
    }

    protected void handle (String line) {
        if (handler == null) {
            return;
        }
        JsonObject obj = (JsonObject)new JsonParser().parse(line);
        JsonElement name = obj.get("name");
        if (name == null) {
            Flog.warn("No name for receive, ignoring");
            return;
        }
        String requestName = name.getAsString();
        if (requestName.equals("pong")) {
            if (timeout != null) {
                timeout.cancel();
            }

            timeout = handler.context.setTimeout(5000, new Runnable() {
                @Override
                public void run() {
                    setTimeout();
                }
            });
            return;
        }
        handler.on_data(requestName, obj);
    }

    protected void connect() throws IOException {
        final FlooUrl flooUrl = handler.getUrl();
        SSLContext sslContext = Utils.createSSLContext();
        if (sslContext == null) {
            Utils.errorMessage("I can't do SSL.", handler.getProject());
            return;
        }

        cleanUp();

        try{
            socket = (SSLSocket) sslContext.getSocketFactory().createSocket(flooUrl.host, flooUrl.port);
            Integer SOCKET_TIMEOUT = 0;
            socket.setSoTimeout(SOCKET_TIMEOUT);
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);
        } catch (IOException e) {
            Flog.warn("Error connecting %s", e);
            return;
        }
        out = new OutputStreamWriter(socket.getOutputStream());
        writeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeLoop();
            }
        }, "FlooConn Write Thread");
        writeThread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        writeThread.start();
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
            } catch (SocketTimeoutException e) {
                Flog.info("Caught timeout on socket (%s)", socket.isClosed());
                return;
            }

            if (line == null) {
                return;
            }

            retries = MAX_RETRIES;
            delay = INITIAL_RECONNECT_DELAY;
            handle(line);
        }
    }
}
