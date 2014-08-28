package floobits.common.protocol.json.send;

import floobits.common.Constants;
import floobits.common.protocol.Base;
import floobits.impl.ApplicationImpl;

public abstract class InitialBase implements Base {
    public String platform = System.getProperty("os.name");
    public String version = Constants.version;
    public String client = ApplicationImpl.getClientName();
}
