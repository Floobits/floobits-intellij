package floobits.common;

import floobits.FlooContext;
import floobits.common.protocol.FlooPatch;
import floobits.utilities.Flog;
import io.fletty.util.concurrent.ScheduledFuture;

import java.util.regex.Pattern;


public abstract class Buf <T, T2> {
    protected static final Pattern NEW_LINE = Pattern.compile("\\r\\n?", Pattern.DOTALL);
    public String path;
    public final Integer id;
    public volatile String md5;
    public volatile T buf;
    public Encoding encoding;
    public ScheduledFuture timeout;
    public boolean forced_patch = false;
    protected FlooContext context;
    protected OutboundRequestHandler outbound;

    abstract protected T getText();
    abstract protected T getText(T2 f);
    abstract public void load();
    abstract public void updateView();
    abstract public void set (String s, String md5);
    abstract public void patch (FlooPatch res);
    abstract public void send_patch (T2 f);
    abstract public String serialize();

    public Buf(String path, Integer id, T buf, String md5, FlooContext context, OutboundRequestHandler outbound) {
        this.id = id;
        this.path = path;
        this.buf = buf;
        this.md5 = md5;
        this.context = context;
        this.outbound = outbound;
    }

    public void write() {
        if (!isPopulated()) {
            Flog.warn("Unable to write %s because it's not populated yet.", path);
            return;
        }
        updateView();
    }

    synchronized public void set (T s, String md5) {
        buf = s;
        this.md5 = md5;
    }

    public void cancelTimeout () {
        if (timeout != null) {
            Flog.log("canceling timeout for %s", path);
            timeout.cancel(false);
            timeout = null;
        }
    }

    public static boolean isBad(Buf b) {
        return (b == null || !b.isPopulated());
    }

    public Boolean isPopulated() {
        return this.id != null && this.buf != null;
    }

    public String toString() {
        return String.format("id: %s file: %s", id, path);
    }
}

