package floobits.common.protocol.json.receive;

import floobits.common.protocol.Base;

public class RenameBuf implements Base {
    public Integer id;
    public String name = "rename_buf";
    public String path;

    public RenameBuf(Integer id, String path) {
        this.id = id;
        this.path = path;
    }
}
