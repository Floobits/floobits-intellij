package floobits;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.FileOutputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;


import com.intellij.openapi.diagnostic.Logger;

import floobits.Shared;


class Utils {
    private static Logger Log = Logger.getInstance(Listener.class);
    
    public static String pathJoin(String... paths)
    {
        File file = new File(paths[0]);

        for (int i = 1; i < paths.length ; i++) {
            file = new File(file, paths[i]);
        }
        return file.getPath();
    }

    public static String defaultBaseDir () {
        return Utils.pathJoin(System.getProperty("user.home"), "floobits");
    }
}
