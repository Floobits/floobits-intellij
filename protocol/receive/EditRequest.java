package floobits.common.protocol.receive;

import java.io.Serializable;
import java.util.ArrayList;

public class EditRequest implements Serializable {
    String name = "request_perms";
    public ArrayList<String> perms;

    public EditRequest (ArrayList<String> perms) {
        this.perms = perms;
    }
}
