package floobits.common.protocol.json.send;

import floobits.common.Utils;

public class PermsChange extends InitialBase{
    String name = "perms";
    public int req_id = Utils.getRequestId();
    String action;
    int user_id;
    String[] perms;

    public PermsChange(String action, int userId, String[] perms) {
        this.action = action;
        this.user_id = userId;
        this.perms = perms;
    }
}
