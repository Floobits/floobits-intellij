package floobits.common.protocol.json.send;

import floobits.common.Utils;

public class FlooRequestCredentials extends InitialBase {
    String name = "request_credentials";
    public int req_id = Utils.getRequestId();
    String username = System.getProperty("user.name");
    String token;

    public FlooRequestCredentials(String token) {
        this.token = token;
    }
}
