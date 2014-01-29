package floobits;


import com.intellij.openapi.application.ApplicationInfo;

import java.io.Serializable;

public class FlooNewAccount implements Serializable {
    // TODO: Share this code with FlooAuth
    String name = "create_user";
    String username = System.getProperty("user.name");
    String client = ApplicationInfo.getInstance().getVersionName();
    public String platform = System.getProperty("os.name");
    public String version = "0.10";

    public FlooNewAccount() {
        // Do nothing.
    }
}

