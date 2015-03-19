package floobits.common.protocol.json.send;

import floobits.common.Utils;
import floobits.common.protocol.Base;

public class GetBuf implements Base {
    public String name = "get_buf";
    public int req_id = Utils.getRequestId();
    public Integer id;

    public GetBuf(Integer buf_id) {
        this.id = buf_id;
    }
}
