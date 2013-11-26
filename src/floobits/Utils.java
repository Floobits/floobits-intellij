package floobits;

import java.io.File;

import floobits.Shared;


class Utils {
    
    public static String pathJoin(String... paths)
    {
        File file = new File(paths[0]);

        for (int i = 1; i < paths.length ; i++) {
            file = new File(file, paths[i]);
        }

        return file.getPath();
    }

    public static String defaultBaseDir() {
        return Utils.pathJoin(System.getProperty("user.home", "floobits"));
    }
}
