package floobits;

import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;

public class DotFloo {
    String path;

    public static File path(String base_dir) {
        return new File(FilenameUtils.concat(base_dir, ".floo"));
    }

    public static FlooUrl read(String base_dir) {
        String url;
        String floo;

        try {
            floo = FileUtils.readFileToString(path(base_dir), "UTF-8");
        } catch (Exception e) {
            Flog.debug("no floo file %s", path(base_dir));
            return null;
        }

        try {
            url = new JsonParser().parse(floo).getAsJsonObject().get("url").getAsString();
            return new FlooUrl(url);
        } catch (Exception e) {
            Flog.error(e);
        }

        return null;
    }

    public static void write(String url) {
        try {
            String flooFile = String.format("{\n    \"url\": \"%s\"\n}", url);
            FileUtils.write(path(Shared.colabDir), flooFile, "UTF-8");
        } catch (Exception e) {
            Flog.error(e);
        }
    }
}
