package floobits.utilities;

import com.intellij.openapi.diagnostic.Logger;

/**
 * Do not add a Log.error statement to this class. Error statements are user visible exceptions. Use
 * Utitls.errorMessage to notify the user of a problem and Flog.warn to log an exception in a way that doesn't
 * disturb the user.
 */
public class Flog {
    public static Logger Log = Logger.getInstance(Flog.class);
    public static void log (String s, Object... args) {
        Log.info(String.format(s, args));
    }
    public static void debug (String s, Object... args) {
        Log.debug(String.format(s, args));
    }
    public static void warn (Throwable e) {
        Log.warn(e);
    }
    public static void warn (String s, Object... args) {
        Log.warn(String.format(s, args));
    }
    public static void info (String s, Object... args) {
        Log.info(String.format(s, args));
    }
}
