package floobits.common.protocol.send;

import floobits.common.protocol.Base;

public class GetBuf extends Base {
    public String name = "get_buf";
    public Integer id;

    public GetBuf(Integer buf_id) {
        this.id = buf_id;
    }
}
