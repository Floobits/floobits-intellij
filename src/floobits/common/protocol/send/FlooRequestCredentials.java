package floobits.common.protocol.send;

import com.intellij.openapi.application.ApplicationInfo;
import floobits.common.protocol.Base;

import java.io.Serializable;

public class FlooRequestCredentials extends Base {
    // TODO: Share this code with FlooAuth and FlooNewAccount
    String name = "request_credentials";
    String username = System.getProperty("user.name");
    String client = ApplicationInfo.getInstance().getVersionName();
    public String platform = System.getProperty("os.name");
    String token;
    public String version = "0.10";

    public FlooRequestCredentials(String token) {
        this.token = token;
    }
}
