package floobits.common.protocol.send;

import floobits.common.Settings;

public class FlooAuth extends InitialBase {
    // TODO: Share this code with NewAccount
    public String username;
    public String api_key;
    public String secret;

    public String room;
    public String room_owner;
    public String[] supported_encodings = { "utf8", "base64" };

    public FlooAuth (Settings settings, String owner, String workspace) {
        this.username = settings.get("username");
        this.api_key = settings.get("api_key");
        this.room = workspace;
        this.room_owner = owner;
        this.secret = settings.get("secret");
    }
}
