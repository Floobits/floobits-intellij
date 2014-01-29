package floobits.common.protocol.receive;

import floobits.common.Buf;

import java.io.Serializable;

/**
 * Created by kans on 1/28/14.
 */
public class FlooSetBuf implements Serializable {
    public String name = "set_buf";
    public Integer id;
    public String buf;
    public String md5;
    public String encoding;

    public FlooSetBuf (Buf buf) {
        this.md5 = buf.md5;
        this.id = buf.id;
        this.buf = buf.serialize();
        this.encoding = buf.encoding.toString();
    }
}
