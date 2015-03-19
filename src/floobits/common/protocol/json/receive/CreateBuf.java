package floobits.common.protocol.json.receive;

import floobits.common.protocol.Base;
import floobits.common.protocol.buf.Buf;
import org.apache.commons.io.FilenameUtils;

public class CreateBuf implements Base {
    public String name = "create_buf";
    public String buf;
    public String path;
    public String md5;
    public String encoding;

    public CreateBuf(Buf buf) {
        this.path = FilenameUtils.separatorsToUnix(buf.path);
        this.buf = buf.serialize();
        this.md5 = buf.md5;
        this.encoding = buf.encoding.toString();
    }
}
