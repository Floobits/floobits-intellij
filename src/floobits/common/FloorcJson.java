package floobits.common;

import floobits.utilities.Flog;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by kans on 5/29/14.
 */
public class FloorcJson implements Serializable {
    public HashMap<String, HashMap<String, String>> auth;
    public Boolean debug;
    public String share_dir;

    public static FloorcJson getFloorcJsonFromSettings () {
        FloorcJson floorcJson = null;
        try {
            floorcJson = Settings.get();
        } catch (Exception e) {
            Flog.warn(e);
        }
        if (floorcJson == null) {
            floorcJson = new FloorcJson();
        }
        if (floorcJson.auth == null) {
            floorcJson.auth = new HashMap<String, HashMap<String, String>>();
        }
        return floorcJson;
    }
}
