package floobits.common.protocol.json.send;

import floobits.common.Utils;
import floobits.common.protocol.Base;

public class FlooKick implements Base {
    String name = "kick";
    public int req_id = Utils.getRequestId();
    int user_id;

    public FlooKick(int userId) {
        this.user_id = userId;
    }
}
