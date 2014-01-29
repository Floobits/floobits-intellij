package floobits.common.protocol;

import com.intellij.openapi.application.ApplicationInfo;
import floobits.common.Settings;

import java.io.Serializable;

/**
 * Created by kans on 1/28/14.
 */
public class FlooAuth implements Serializable {
    // TODO: Share this code with FlooNewAccount
    static String clientName = ApplicationInfo.getInstance().getVersionName();
    public String username;
    public String api_key;
    public String secret;

    public String room;
    public String room_owner;
    public String client = FlooAuth.clientName;
    public String platform = System.getProperty("os.name");
    public String version = "0.10";
    public String[] supported_encodings = { "utf8", "base64" };

    public FlooAuth (Settings settings, String owner, String workspace) {
        this.username = settings.get("username");
        this.api_key = settings.get("api_key");
        this.room = workspace;
        this.room_owner = owner;
        this.secret = settings.get("secret");
    }
}
