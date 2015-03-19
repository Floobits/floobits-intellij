package floobits.common.protocol.json.receive;

import floobits.common.Utils;
import floobits.common.protocol.Base;


public class Perms implements Base {
    public String name = "perms";
    public int req_id = Utils.getRequestId();
    public int user_id;
    public String[] perms;
    public String action;
}
