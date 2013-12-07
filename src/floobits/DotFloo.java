package floobits;

import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;

public class DotFloo {
    String path;

    public static File path() {
        return new File(FilenameUtils.concat(Shared.colabDir, ".floo"));
    }

    public static FlooUrl read() {
        String url;
        String floo;
        try {
            floo = FileUtils.readFileToString(path(), "UTF-8");
        } catch (Exception e) {
            Flog.debug("no floo file %s", path());
            return null;
        }

        try {
            url = new JsonParser().parse(floo).getAsJsonObject().get("url").getAsString();
            FlooUrl flooUrl = new FlooUrl(url);
            return flooUrl;
        } catch (Exception e) {
            Flog.error(e);
        }

        return null;
    }

    public static void write(String url) {
        try {
            String flooFile = String.format("{\n    \"url\": \"%s\"\n}", url);
            FileUtils.write(path(), flooFile, "UTF-8");
        } catch (Exception e) {
            Flog.error(e);
        }
    }
}
