package floobits;

import org.apache.commons.io.FilenameUtils;
import floobits.Flog;

class Utils {

    public static String defaultBaseDir () {
        return FilenameUtils.concat(System.getProperty("user.home"), "floobits");
    }
}
