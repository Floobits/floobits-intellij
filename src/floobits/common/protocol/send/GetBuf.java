package floobits.common.protocol.send;

import java.io.Serializable;

/**
 * Created by kans on 1/28/14.
 */
public class GetBuf implements Serializable {
    public String name = "get_buf";
    public Integer id;

    public GetBuf(Integer buf_id) {
        this.id = buf_id;
    }
}
