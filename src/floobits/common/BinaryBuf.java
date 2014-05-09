package floobits.common;

import com.intellij.openapi.vfs.VirtualFile;
import floobits.FlooContext;
import floobits.Listener;
import floobits.common.handlers.FlooHandler;
import floobits.common.protocol.FlooPatch;
import floobits.utilities.Flog;
import floobits.utilities.ThreadSafe;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.nio.charset.Charset;

public class BinaryBuf extends Buf <byte[]> {

    public BinaryBuf(String path, Integer id, byte[] buf, String md5, FlooContext context, OutboundRequestHandler outbound) {
        super(path, id, buf, md5, context, outbound);
        this.encoding = Encoding.BASE64;
    }

    synchronized public void read () {
        VirtualFile virtualFile = this.getVirtualFile();
        if (virtualFile == null) {
            Flog.warn("Couldn't get virtual file in readFromDisk %s", this);
            return;
        }
        try {
            this.buf = virtualFile.contentsToByteArray();
        } catch (IOException e) {
            Flog.warn("Could not get byte array contents for file %s", this);
            return;
        }
        this.md5 = DigestUtils.md5Hex(this.buf);
    }

    public void write() {
        ThreadSafe.write(context, new Runnable() {
            @Override
            public void run() {
                VirtualFile virtualFile = getVirtualFile();
                if (virtualFile == null) {
                    virtualFile = createFile();
                    if (virtualFile == null) {
                        Utils.errorMessage("Unable to write file.", context.project);
                        return;
                    }
                }
                FlooHandler flooHandler = context.getFlooHandler();
                if (flooHandler == null) {
                    return;
                }
                try {
                    Listener.flooDisable();
                    virtualFile.setBinaryContent(buf);
                } catch (IOException e) {
                    Flog.warn("Writing binary content to disk failed. %s", path);
                } finally {
                    Listener.flooEnable();
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

    public void send_patch(VirtualFile virtualFile) {
        FlooHandler flooHandler = context.getFlooHandler();
        if (flooHandler == null) {
            return;
        }
        byte[] contents;
        try {
            contents = virtualFile.contentsToByteArray();
        } catch (IOException e) {
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
