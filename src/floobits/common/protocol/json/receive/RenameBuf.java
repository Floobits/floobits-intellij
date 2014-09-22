package floobits.common.protocol.json.receive;

import floobits.common.protocol.Base;
import org.apache.commons.io.FilenameUtils;

public class RenameBuf implements Base {
    public Integer id;
    public String name = "rename_buf";
    public String path;

    public RenameBuf(Integer id, String path) {
        this.id = id;
        // This should already have unix separators but just to make sure
        this.path = FilenameUtils.separatorsToUnix(path);
    }
}
