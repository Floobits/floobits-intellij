package floobits.utilities;

import com.intellij.openapi.diagnostic.Logger;


public class Flog {
    public static Logger Log = Logger.getInstance(Flog.class);
    public static void log (String s, Object... args) {
        Log.info(String.format(s, args));
    }
    public static void debug (String s, Object... args) {
        Log.debug(String.format(s, args));
    }
    public static void throwAHorribleBlinkingErrorAtTheUser (String s, Object... args) {
        Log.error(String.format(s, args));
    }
    public static void throwAHorribleBlinkingErrorAtTheUser (Throwable e) {
        Log.error(e);
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
