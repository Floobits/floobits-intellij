package floobits;

import org.apache.commons.io.FilenameUtils;
import com.intellij.openapi.diagnostic.Logger;

class Utils {
    private static Logger Log = Logger.getInstance(Listener.class);

    public static String defaultBaseDir () {
        return FilenameUtils.concat(System.getProperty("user.home"), "floobits");
    }
}
