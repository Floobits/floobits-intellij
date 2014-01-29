package floobits.common.protocol.receive;

import java.io.Serializable;

/**
 * Created by kans on 1/28/14.
 */
public class RenameBuf implements Serializable {
    public Integer id;
    public String name = "rename_buf";
    public String path;

    public RenameBuf(Integer id, String path) {
        this.id = id;
        this.path = path;
    }
}
