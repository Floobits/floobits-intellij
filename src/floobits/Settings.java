package floobits;

import java.io.*;
import java.util.HashMap;
import com.intellij.openapi.diagnostic.Logger;

class Settings {
    protected HashMap<String, String> settings;
    private static Logger Log = Logger.getInstance(Settings.class);

    public Settings () {
        String userHome = System.getProperty( "user.home" );
        String floorcPath = Path.combine(userHome, ".floorc");
        BufferedReader br = null;
        this.settings = new HashMap<String, String>();
        try {
            br = new BufferedReader(new FileReader(floorcPath));
            String line = br.readLine();

            while (line != null) {
                Log.debug(line);
                if (line.length() < 1 || line.substring(0, 1).equals("#")){
                    line = br.readLine();
                    continue;
                }
                String[] shit = line.split(" ");
                this.settings.put(shit[0], shit[1]);
                line = br.readLine();
            }
        } catch (Exception e) {
            Log.error(e);
        }
    }

    public String get(String k) {
        return this.settings.get(k);
    }
}
