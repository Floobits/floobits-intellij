package floobits.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import floobits.common.interfaces.FlooContext;
import floobits.utilities.Flog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;


public class Settings {
    public static String floorcJsonPath = FilenameUtils.concat(System.getProperty("user.home"), ".floorc.json");

    public static FloorcJson get() throws Exception {
        File f = new File(floorcJsonPath);
        String string;
        try {
            string = FileUtils.readFileToString(f, "UTF-8");
        } catch (IOException e) {
            Flog.warn(e);
            return new FloorcJson();
        }
        try {
            return new Gson().fromJson(string, (Type) FloorcJson.class);
        } catch (JsonSyntaxException e) {
           throw new Exception("Invalid JSON.");
       }
    }

    public static void write(FlooContext context, FloorcJson floorcJson) {
        File file = new File(floorcJsonPath);
        if (!file.exists()) {
            boolean newFile;
            try {
                newFile = file.createNewFile();
            } catch (IOException e) {
                context.errorMessage("Can't write new ~/.floorc.json");
                return;
            }
            if (!newFile) {
                context.errorMessage("Can't write new ~/.floorc.json");
                return;
            }
        }

        try {
            FileUtils.write(file, new GsonBuilder().setPrettyPrinting().create().toJson(floorcJson));
        } catch (IOException e) {
            Flog.warn(e);
            context.errorMessage("Can't write new ~/.floorc.json");
        }
    }

    public static Boolean isAuthComplete(HashMap<String, String> settings) {
        return (settings.get("secret") != null && (settings.get("username") != null || settings.get("api_key") != null));
    }

    public static Boolean canFloobits() {
        HashMap<String, HashMap<String, String>> auth = null;
        try {
            auth = get().auth;
        } catch (Throwable e) {
            return false;
        }
        for (String host : auth.keySet()) {
            if (isAuthComplete(auth.get(host))) {
                return true;
            }
        }
        return false;
    }
}
