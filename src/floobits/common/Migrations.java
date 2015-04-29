package floobits.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import floobits.utilities.Flog;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kans on 5/29/14.
 */
public class Migrations {

    public static void migrateFloorc() {
        File file = new File(Settings.floorcJsonPath);
        if (file.exists()) {
            return;
        }
        BufferedReader br;
        FloorcJson floorcJson = new FloorcJson();
        String floorcPath = FilenameUtils.concat(System.getProperty("user.home"), ".floorc");
        HashMap<String, String> settings = new HashMap<String, String>();
        FileReader fileReader;

        try {
            fileReader = new FileReader(floorcPath);
        } catch (FileNotFoundException e) {
            return;
        }
        try {
            br = new BufferedReader(fileReader);
            String line = br.readLine();

            while (line != null) {
                if (line.length() < 1 || line.substring(0, 1).equals("#")) {
                    line = br.readLine();
                    continue;
                }
                String[] shit = line.split(" ");
                settings.put(shit[0].toUpperCase(), shit[1]);
                line = br.readLine();
            }
        } catch (Throwable e) {
            Flog.info("Got an exception migrating floorc");
            Flog.error(e);
        }
        String default_host = settings.get("DEFAULT_HOST");
        default_host = default_host != null ? default_host : Constants.defaultHost;
        HashMap<String, String> auth = new HashMap<String, String>();
        floorcJson.auth = new HashMap<String, HashMap<String, String>>();
        floorcJson.auth.put(default_host, auth);
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.equals("USERNAME") || key.equals("SECRET") || key.equals("API_KEY")) {
                auth.put(key.toLowerCase(), value);
                continue;
            }
            if (key.equals("SHARE_DIR")) {
                floorcJson.share_dir = value;
                continue;
            }
            if (key.equals("DEBUG")) {
                Integer i;
                try {
                    i = Integer.parseInt(value);
                } catch (Throwable e) {
                    continue;
                }
                floorcJson.debug = i != 0;
            }
        }
        if (floorcJson.share_dir == null) {
            floorcJson.share_dir = FilenameUtils.concat(System.getProperty("user.home"), "floobits");
        }
        if (floorcJson.debug == null) {
            floorcJson.debug = false;
        }
        PrintWriter writer = null;
        if (file.exists()) {
            return;
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            return;
        }
        try {
            writer = new PrintWriter(file, "UTF-8");
        } catch (FileNotFoundException e) {
            Flog.errorMessage("Can't write new .floorc", null);
        } catch (UnsupportedEncodingException e) {
            Flog.errorMessage("Can't write new .floorc", null);
        }
        if (writer == null) {
            return;
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String data = gson.toJson(floorcJson);

        writer.print(data);
        writer.close();
    }
}
