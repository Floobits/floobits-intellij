package floobits.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import floobits.utilities.Flog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.util.HashMap;

public class DotFloo {
    private class FlooDot implements Serializable {
        String url;
        HashMap<String, String> hooks;
    }

    public static File path(String base_dir) {
        return new File(FilenameUtils.concat(base_dir, ".floo"));
    }

    private static FlooDot parse(String base_dir) {
        String floo;

        try {
            floo = FileUtils.readFileToString(path(base_dir), "UTF-8");
        } catch (Exception e) {
            Flog.debug("no floo file %s", path(base_dir));
            return null;
        }

        try {
            return new Gson().fromJson(floo, (Type) FlooDot.class);
        } catch (Exception e) {
            Flog.warn(e);
        }
        return null;
    }
    public static FlooUrl read(String base_dir) {
        FlooDot flooDot = parse(base_dir);
        if (flooDot == null)
            return null;

        try {
            return new FlooUrl(flooDot.url);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static void write(String base_dir, String url) {
        FlooDot flooDot = parse(base_dir);
        if (flooDot == null)
            return;

        flooDot.url = url;

        try {
            FileUtils.write(path(base_dir), new GsonBuilder().setPrettyPrinting().create().toJson(flooDot), "UTF-8");
        } catch (Exception e) {
            Flog.warn(e);
        }
    }
}
