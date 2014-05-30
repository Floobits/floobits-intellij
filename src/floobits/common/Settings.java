package floobits.common;

import com.google.gson.Gson;
import floobits.FlooContext;
import floobits.utilities.Flog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;


public class Settings {
    public static String floorcJsonPath = FilenameUtils.concat(System.getProperty("user.home"), ".floorc.json");

    public static FloorcJson get() {
        File f = new File(floorcJsonPath);
        String string;
        try {
            string = FileUtils.readFileToString(f, "UTF-8");
        } catch (IOException e) {
            Flog.warn(e);
            return new FloorcJson();
        }
        return new Gson().fromJson(string, (Type)FloorcJson.class);
    }

    public static void write(FlooContext context, FloorcJson floorcJson) {
        File file = new File(floorcJsonPath);
        if (!file.exists()) {
            boolean newFile;
            try {
                newFile = file.createNewFile();
            } catch (IOException e) {
                context.errorMessage("Can't write new .floorc");
                return;
            }
            if (!newFile) {
                context.errorMessage("Can't write new .floorc");
                return;
            }
        }

        try {
            FileUtils.write(file, new Gson().toJson(floorcJson));
        } catch (IOException e) {
            Flog.warn(e);
            context.errorMessage("Can't write new .floorc");
        }
    }

    public static Boolean isComplete(HashMap<String, String> settings) {
        return (settings.get("secret") != null && (settings.get("username") != null || settings.get("api_key") != null));
    }
}
