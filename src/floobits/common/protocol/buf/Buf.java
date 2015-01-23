package floobits.common.protocol.buf;

import floobits.common.Encoding;
import floobits.common.OutboundRequestHandler;
import floobits.common.interfaces.IContext;
import floobits.common.interfaces.IDoc;
import floobits.common.interfaces.IFile;
import floobits.common.protocol.FlooPatch;
import floobits.utilities.Flog;
import io.fletty.util.concurrent.ScheduledFuture;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public abstract class Buf <T> {
    public String path;
    public Integer id;
    public volatile String md5;
    public volatile T buf;
    public Encoding encoding;
    public ScheduledFuture timeout;
    public boolean forced_patch = false;
    protected final IContext context;
    protected OutboundRequestHandler outbound;

    public Buf(String path, Integer id, T buf, String md5, IContext context, OutboundRequestHandler outbound) {
        this.id = id;
        this.path = path;
        this.buf = buf;
        this.md5 = md5;
        this.context = context;
        this.outbound = outbound;
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

    protected IFile getVirtualFile() {
        return context.iFactory.findFileByPath(context.absPath(this.path));
    }


    protected IFile getOrCreateFile() {
        return context.iFactory.getOrCreateFile(context.absPath(this.path));
    }


    protected IDoc getVirtualDoc() {
        IFile virtualFile = getVirtualFile();
        if (virtualFile == null) {
            Flog.warn("Can't get virtual file to read from disk %s", this);
            return null;
        }

        return context.iFactory.getDocument(virtualFile);
    }

    public String toString() {
        return String.format("id: %s file: %s", id, path);
    }


    abstract public void read ();
    abstract public void write();
    abstract public void set (String s, String md5);
    abstract public void patch (FlooPatch res);
    abstract public void send_patch (IFile virtualFile);
    abstract public String serialize();

    public static Buf createBuf(String path, Integer id, Encoding enc, String md5, IContext context, OutboundRequestHandler outbound) {
        if (enc == Encoding.BASE64) {
            return new BinaryBuf(path, id, null, md5, context, outbound);
        }
        return new TextBuf(path, id, null, md5, context, outbound);
    }
    public static Buf createBuf(IFile virtualFile, IContext context, OutboundRequestHandler outbound) {
        try {
            byte[] originalBytes = virtualFile.getBytes();
            if (originalBytes == null) {
                Flog.warn("Got null bytes (I/O error) for %s, cannot digest a null", virtualFile);
                originalBytes = new byte[]{};
            }
            String encodedContents = new String(originalBytes, "UTF-8");
            byte[] decodedContents = encodedContents.getBytes("UTF-8");
            String filePath = context.toProjectRelPath(virtualFile.getPath());
            if (Arrays.equals(decodedContents, originalBytes)) {
                IDoc doc = context.iFactory.getDocument(virtualFile);
                String contents = doc == null ? encodedContents : doc.getText();
                String md5 = DigestUtils.md5Hex(contents);
                return new TextBuf(filePath, null, contents, md5, context, outbound);
            } else {
                Flog.info("Creating binary buffer for %s", virtualFile);
                String md5 = DigestUtils.md5Hex(originalBytes);
                return new BinaryBuf(filePath, null, originalBytes, md5, context, outbound);
            }
        } catch (IOException e) {
            Flog.warn("Error getting virtual file contents in createBuf %s", virtualFile);
        }
        return null;
    }
}

