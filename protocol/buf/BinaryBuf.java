package floobits.common.protocol.buf;

import floobits.common.Encoding;
import floobits.common.OutboundRequestHandler;
import floobits.common.protocol.handlers.FlooHandler;
import floobits.common.interfaces.IContext;
import floobits.common.interfaces.IFile;
import floobits.common.protocol.FlooPatch;
import floobits.utilities.Flog;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import java.nio.charset.Charset;

public class BinaryBuf extends Buf <byte[]> {

    public BinaryBuf(String path, Integer id, byte[] buf, String md5, IContext context, OutboundRequestHandler outbound) {
        super(path, id, buf, md5, context, outbound);
        this.encoding = Encoding.BASE64;
    }

    synchronized public void read () {
        IFile virtualFile = getVirtualFile();
        if (virtualFile == null) {
            Flog.warn("Couldn't get virtual file in readFromDisk %s", this);
            return;
        }

        final byte[] bytes = virtualFile.getBytes();
        if (bytes == null) {
            Flog.warn("Could not get byte array contents for file %s", this);
            return;
        }
        buf = bytes;
        md5 = DigestUtils.md5Hex(bytes);
    }

    public void write() {
        context.writeThread(new Runnable() {
            @Override
            public void run() {
                if (!isPopulated()) {
                    Flog.warn("Unable to write %s because it's not populated yet.", path);
                    return;
                }
                IFile virtualFile = getVirtualFile();
                if (virtualFile == null) {
                    virtualFile = createFile();
                    if (virtualFile == null) {
                        context.errorMessage("Unable to write file. virtualFile is null.");
                        return;
                    }
                }
                FlooHandler flooHandler = context.getFlooHandler();
                if (flooHandler == null) {
                    return;
                }
                synchronized (context) {
                    try {
                        context.setListener(false);
                        if (!virtualFile.setBytes(buf)) {
                            Flog.warn("Writing binary content to disk failed. %s", path);
                        }
                    } finally {
                        context.setListener(true);
                    }
                }
            }
        });
    }

    synchronized public void set (String s, String md5) {
        buf = s == null ? new byte[]{} : Base64.decodeBase64(s.getBytes(Charset.forName("UTF-8")));
        this.md5 = md5;
    }

    synchronized public void set (byte[] s, String md5) {
        buf = s;
        this.md5 = md5;
    }

    public String serialize() {
        return new String(Base64.encodeBase64(buf));
    }

    public void patch(FlooPatch res) {
        FlooHandler flooHandler = context.getFlooHandler();
        if (flooHandler == null) {
            return;
        }
        flooHandler.outbound.getBuf(this.id);
        set((byte[]) null, null);
    }

    public void send_patch(IFile virtualFile) {
        FlooHandler flooHandler = context.getFlooHandler();
        if (flooHandler == null) {
            return;
        }
        byte[] contents = virtualFile.getBytes();
        if (contents == null) {
            Flog.warn("Couldn't read contents of binary file. %s", virtualFile);
            return;
        }
        String after_md5 = DigestUtils.md5Hex(contents);
        if (md5.equals(after_md5)) {
            Flog.debug("Binary file change event but no change in md5 %s", virtualFile);
            return;
        }
        set(contents, after_md5);
        flooHandler.outbound.setBuf(this);
    }
}
