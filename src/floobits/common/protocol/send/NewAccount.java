package floobits.common.protocol.send;


import com.intellij.openapi.application.ApplicationInfo;
import floobits.common.protocol.Base;

public class NewAccount extends Base {
    // TODO: Share this code with FlooAuth
    String name = "create_user";
    String username = System.getProperty("user.name");
    String client = ApplicationInfo.getInstance().getVersionName();
    public String platform = System.getProperty("os.name");
    public String version = "0.10";

    public NewAccount() {
        // Do nothing.
    }
}

