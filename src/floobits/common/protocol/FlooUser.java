package floobits.common.protocol;

import java.io.Serializable;

public class FlooUser implements Serializable {
    public String[] perms;
    public String client;
    public String platform;
    public Integer user_id;
    public String username;
    public String version;
    public String gravatar;
}
