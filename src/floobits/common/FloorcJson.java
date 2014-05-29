package floobits.common;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by kans on 5/29/14.
 */
public class FloorcJson implements Serializable {
    public HashMap<String, HashMap<String, String>> auth;
    public Boolean debug;
    public String share_dir;
}
