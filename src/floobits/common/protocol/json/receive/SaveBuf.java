package floobits.common.protocol.json.receive;

import floobits.common.Utils;
import floobits.common.protocol.Base;

public class SaveBuf implements Base {
    public Integer id;
    public String name = "saved";
    public int req_id = Utils.getRequestId();

    public SaveBuf(Integer id) {
        this.id = id;
    }
}
