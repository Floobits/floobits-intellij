package floobits.common.protocol.json.send;

import floobits.common.Constants;
import floobits.common.protocol.Base;

public abstract class InitialBase implements Base {
    public String platform = System.getProperty("os.name");
    public String version = Constants.version;
    public static String client = "";
}
