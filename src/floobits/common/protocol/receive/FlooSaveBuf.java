package floobits.common.protocol.receive;

import java.io.Serializable;

/**
 * Created by kans on 1/28/14.
 */
public class FlooSaveBuf implements Serializable {
    public Integer id;
    public String name = "saved";

    public FlooSaveBuf(Integer id) {
        this.id = id;
    }
}
