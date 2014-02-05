package floobits.common;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import com.intellij.openapi.project.Project;
import floobits.utilities.Flog;
import org.apache.commons.io.FilenameUtils;


public class Settings {
    protected Project project;
    protected HashMap<String, String> settings;
    public static String floorcPath = FilenameUtils.concat(System.getProperty("user.home"), ".floorc");

    public Settings(Project project) {
        this.project = project;
        BufferedReader br;
        this.settings = new HashMap<String, String>();
        try {
            br = new BufferedReader(new FileReader(floorcPath));
            String line = br.readLine();

            while (line != null) {
                if (line.length() < 1 || line.substring(0, 1).equals("#")){
                    line = br.readLine();
                    continue;
                }
                String[] shit = line.split(" ");
                this.settings.put(shit[0], shit[1]);
                line = br.readLine();
            }
        } catch (Exception e) {
            Flog.info("Got an exception %s", e);
        }
    }

    public String get(String k) {
        return this.settings.get(k);
    }
    
    public void set(String k, String v) {
        this.settings.put(k, v);
    }
    
    public void write() {
        PrintWriter writer = null;
        File file = new File(floorcPath);
        try {
            file.createNewFile();
        } catch (IOException e) {
            Utils.error_message("Can't write new .floorc", project);
            return;
        }
        try {
            writer = new PrintWriter(file, "UTF-8");
        } catch (FileNotFoundException e) {
            Utils.error_message("Can't write new .floorc", project);
        } catch (UnsupportedEncodingException e) {
            Utils.error_message("Can't write new .floorc", project);
        }
        for (Map.Entry<String, String> setting : this.settings.entrySet()) {
            writer.println(String.format("%s %s", setting.getKey(), setting.getValue()));
        }
        writer.close();
    }

    public Boolean isComplete() {
        return (settings.get("secret") != null && (settings.get("username") != null || settings.get("api_key") != null));
    }
}
