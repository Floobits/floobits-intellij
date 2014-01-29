package floobits.common.protocol;

import java.io.Serializable;

/**
 * Created by kans on 1/28/14.
 */
public class User implements Serializable {
    public String[] perms;
    public String client;
    public String platform;
    public Integer user_id;
    public String username;
    public String version;
}
