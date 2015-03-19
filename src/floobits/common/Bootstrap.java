package floobits.common;

import floobits.utilities.Flog;

import java.util.Set;

public class Bootstrap {
    public static boolean bootstrap(String editor, String major, String minor, String pluginVersion) {
        //        avoid throwing at all costs because the plugin is disabled!
        boolean createAccount = false;
        try {
            Migrations.migrateFloorc();
            FloorcJson floorcJson = null;
            try {
                floorcJson = Settings.get();
            } catch (Throwable e) {
                Flog.warn(e);
            }
            Set<String> strings = null;
            if (floorcJson != null && floorcJson.auth != null) {
                strings = floorcJson.auth.keySet();
            }
            if ((strings != null ? strings.size() : 0) == 1) {
                Constants.defaultHost = (String) strings.toArray()[0];
            }
            if (!Settings.canFloobits()) {
                createAccount = true;
            }
            if (floorcJson != null && floorcJson.MAX_ERROR_REPORTS != null) {
                API.maxErrorReports = floorcJson.MAX_ERROR_REPORTS;
            }
            String userAgent = String.format("%s-%s-%s %s (%s-%s)", editor, major, minor, pluginVersion, System.getProperty("os.name"), System.getProperty("os.version"));
            CrashDump.setUA(userAgent, editor);
        } catch (Throwable e) {
            Flog.warn(e);
            API.uploadCrash(null, null, e);
        }
        return createAccount;
    }
}
