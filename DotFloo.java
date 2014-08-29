package floobits.common;

import com.google.gson.*;
import floobits.utilities.Flog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;

public class DotFloo {
    private static class DotFlooJson implements Serializable {
        String url;
        HashMap<String, String> hooks;
    }

    public static File path(String base_dir) {
        return new File(FilenameUtils.concat(base_dir, ".floo"));
    }

    private static DotFlooJson parse(String base_dir) {
        String floo;

        try {
            floo = FileUtils.readFileToString(path(base_dir), "UTF-8");
        } catch (Throwable e) {
            Flog.debug("no floo file %s", path(base_dir));
            return null;
        }

        try {
            return new Gson().fromJson(floo, (Type) DotFlooJson.class);
        } catch (Throwable e) {
            Flog.warn(e);
        }
        return null;
    }
    public static FlooUrl read(String base_dir) {
        DotFlooJson dotFlooJson = parse(base_dir);
        if (dotFlooJson == null)
            return null;
        try {
            return new FlooUrl(dotFlooJson.url);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static void write(String base_dir, String url) {
        File file = path(base_dir);
        DotFlooJson dotFlooJson = parse(base_dir);
        if (dotFlooJson == null) {
            Flog.warn("DotFloo isn't json.");
            if (file.exists()) {
                return;
            }
            dotFlooJson = new DotFlooJson();
        }
        String json = "{";
        String spaces = "    ";
        String newline = "\n";
        if (dotFlooJson.hooks != null) {
            String[] strings = new String[dotFlooJson.hooks.size()];
            dotFlooJson.hooks.keySet().toArray(strings);
            Arrays.sort(strings);
            json += String.format("%s%s\"hooks\": {", newline, spaces);
            int i;
            for(i=0; i<strings.length-1; i++) {
                String key = strings[i];
                json += String.format("%s%s%s\"%s\": \"%s\",", newline, spaces, spaces, key, dotFlooJson.hooks.get(key));
            }
            String key = strings[i];
            json += String.format("%s%s%s\"%s\": \"%s\"", newline, spaces, spaces, key, dotFlooJson.hooks.get(key));
            json += newline + spaces + "},";
        }
        json += String.format("%s%s\"url\": \"%s\"", newline, spaces, url);
        json += newline + "}";
        try {
            FileUtils.write(path(base_dir), json, "UTF-8");
        } catch (Throwable e) {
            Flog.warn(e);
        }
    }
}
