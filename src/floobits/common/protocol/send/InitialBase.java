package floobits.common.protocol.send;

import com.intellij.openapi.application.ApplicationInfo;
import floobits.common.protocol.Base;

public abstract class InitialBase extends Base {
    public String platform = System.getProperty("os.name");
    public String version = "0.10";
    String client = ApplicationInfo.getInstance().getVersionName();
}
