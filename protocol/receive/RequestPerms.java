package floobits.common.protocol.receive;

import floobits.common.protocol.Base;

public class RequestPerms implements Base {
    public String name = "request_perms";
    public int user_id;
    public String[] perms;


}
