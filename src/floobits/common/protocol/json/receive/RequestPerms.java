package floobits.common.protocol.json.receive;

import floobits.common.Utils;
import floobits.common.protocol.Base;

public class RequestPerms implements Base {
    public String name = "request_perms";
    public int req_id = Utils.getRequestId();
    public int user_id;
    public String[] perms;
}
