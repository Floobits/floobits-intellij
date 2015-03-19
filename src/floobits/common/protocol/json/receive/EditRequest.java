package floobits.common.protocol.json.receive;

import floobits.common.Utils;
import floobits.common.protocol.Base;

import java.util.ArrayList;

public class EditRequest implements Base {
    String name = "request_perms";
    public int req_id = Utils.getRequestId();
    public ArrayList<String> perms;

    public EditRequest (ArrayList<String> perms) {
        this.perms = perms;
    }
}
