package floobits.common;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by kans on 2/14/14.
 */
public class HTTPWorkspaceRequest implements Serializable {
    public String owner;
    public String name;
    public HashMap<String, String[]> perms =  new HashMap<String, String[]>();

    public HTTPWorkspaceRequest(String owner, String name, boolean notPublic) {
        this.owner = owner;
        this.name = name;
        if (notPublic) {
            perms.put("AnonymousUser", new String[]{});
        } else {
            perms.put("AnonymousUser", new String[]{"view_room"});
        }
    }
}
