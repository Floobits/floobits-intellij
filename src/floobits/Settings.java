package floobits;

import java.io.*;
import java.util.HashMap;
import com.intellij.openapi.diagnostic.Logger;

class Settings extends HashMap<String, String> {
    private static Logger Log = Logger.getInstance(Settings.class);

    public void Settings() {
        String userHome = System.getProperty( "user.home" );
        String floorcPath = Path.combine(userHome, ".floorc");
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(floorcPath));
            String line = br.readLine();

            while (line != null) {
                if (line.length() < 1 || line.substring(0, 1).equals("#")){
                    line = br.readLine();
                    continue;
                }
                String[] shit = line.split(" ");
                this.put(shit[0], shit[1]);
                line = br.readLine();
            }
        } catch (Exception e) {
            Log.error(e);
        }
    }
}
