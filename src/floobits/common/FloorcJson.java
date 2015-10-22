package floobits.common;

import floobits.utilities.Flog;

import java.io.Serializable;
import java.util.HashMap;


public class FloorcJson implements Serializable {
    public HashMap<String, HashMap<String, String>> auth;
    public Boolean debug;
    public Boolean insecure;
    public String share_dir;
    public Integer MAX_ERROR_REPORTS;
    public String DEFAULT_HOST;

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
        if (floorcJson.insecure == null) {
            floorcJson.insecure = false;
        }
        return floorcJson;
    }
}
