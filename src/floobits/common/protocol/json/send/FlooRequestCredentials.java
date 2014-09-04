package floobits.common.protocol.json.send;

public class FlooRequestCredentials extends InitialBase {
    String name = "request_credentials";
    String username = System.getProperty("user.name");
    String token;

    public FlooRequestCredentials(String token) {
        this.token = token;
    }
}
