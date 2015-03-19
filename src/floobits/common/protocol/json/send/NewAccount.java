package floobits.common.protocol.json.send;

import floobits.common.Utils;

public class NewAccount extends InitialBase {
    // TODO: Share this code with FlooAuth
    String name = "create_user";
    public int req_id = Utils.getRequestId();
    String username = System.getProperty("user.name");

    public NewAccount() {
        // Do nothing.
    }
}

