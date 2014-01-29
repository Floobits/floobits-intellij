package floobits.common.protocol.receive;

import floobits.common.protocol.Base;

public class SaveBuf extends Base {
    public Integer id;
    public String name = "saved";

    public SaveBuf(Integer id) {
        this.id = id;
    }
}
