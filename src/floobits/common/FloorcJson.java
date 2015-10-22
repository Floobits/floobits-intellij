package floobits.common;

import floobits.utilities.Flog;

import java.io.Serializable;
import java.util.HashMap;


public class FloorcJson implements Serializable {
    public HashMap<String, HashMap<String, String>> auth;
    public Boolean debug;
    public String share_dir;
    public Integer MAX_ERROR_REPORTS;
    public String DEFAULT_HOST;
    public Boolean unsecured;

    public static FloorcJson getFloorcJsonFromSettings () {
        FloorcJson floorcJson = null;
        try {
            floorcJson = Settings.get();
        } catch (Throwable e) {
            Flog.error(e);
        }
        if (floorcJson == null) {
            floorcJson = new FloorcJson();
        }
        if (floorcJson.auth == null) {
            floorcJson.auth = new HashMap<String, HashMap<String, String>>();
        }
        if (floorcJson.debug == null) {
            floorcJson.debug = false;
        }
        if (floorcJson.unsecured == null) {
            floorcJson.unsecured = false;
        }
        return floorcJson;
    }
}
