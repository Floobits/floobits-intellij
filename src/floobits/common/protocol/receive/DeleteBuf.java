package floobits.common.protocol.receive;

import java.io.Serializable;

/**
 * Created by kans on 1/28/14.
 */
public class DeleteBuf implements Serializable {
    public Integer id;
    public String name = "delete_buf";

    public DeleteBuf(Integer id) {
        this.id = id;
    }
}
