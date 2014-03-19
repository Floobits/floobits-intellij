package floobits.common.protocol.send;

public class NewAccount extends InitialBase {
    // TODO: Share this code with FlooAuth
    String name = "create_user";
    String username = System.getProperty("user.name");

    public NewAccount() {
        // Do nothing.
    }
}

