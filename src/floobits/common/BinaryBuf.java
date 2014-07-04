package floobits.common;

import floobits.FlooContext;
import floobits.common.handlers.FlooHandler;
import floobits.common.protocol.FlooPatch;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import java.nio.charset.Charset;

public abstract class BinaryBuf<T> extends Buf <byte[], T> {

    public BinaryBuf(String path, Integer id, byte[] buf, String md5, FlooContext context, OutboundRequestHandler outbound) {
        super(path, id, buf, md5, context, outbound);
        this.encoding = Encoding.BASE64;
    }

    public abstract byte[] getBytes();

    synchronized public void read () {
        byte[] bytes = getBytes();
        if (bytes == null) {
            return;
        }

        this.md5 = DigestUtils.md5Hex(this.buf);
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
}
