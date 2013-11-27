package floobits;

import com.intellij.openapi.diagnostic.Logger;

class Flog {
    public static Logger Log = Logger.getInstance(Flog.class);
    public static void log (String s, Object... args) {
        Log.info(String.format(s, args));
    }
    public static void debug (String s, Object... args) {
        Log.debug(String.format(s, args));
    }
    public static void error (String s, Object... args) {
        Log.error(String.format(s, args));
    }
    public static void error (Object... args) {
        Log.error(args);
    }
    public static void warn (String s, Object... args) {
        Log.warn(String.format(s, args));
    }
    public static void info (String s, Object... args) {
        Log.info(String.format(s, args));
    }
}
