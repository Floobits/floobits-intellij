package floobits.common.protocol.json.send;

public class FlooAuth extends InitialBase {
    // TODO: Share this code with NewAccount
    public String username;
    public String api_key;
    public String secret;

    public String room;
    public String room_owner;
    public String[] supported_encodings = { "utf8", "base64" };

    public FlooAuth (String username, String api_key, String secret, String owner, String workspace) {
        this.username = username;
        this.api_key = api_key;
        this.secret = secret;
        this.room = workspace;
        this.room_owner = owner;
    }
}
