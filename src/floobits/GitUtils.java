package floobits;

import floobits.utilities.Flog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public class GitUtils {
    public static String branchName(String colabDir) {
        String contents = null;
        String headPath = String.format("%s/.git/HEAD", colabDir);
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(new File(headPath));
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            contents = Charset.defaultCharset().decode(bb).toString();
        } catch (FileNotFoundException e) {
            Flog.info("Error git branch file not found");
            return null;
        } catch (IOException e) {
            Flog.error("Some exception attempting to get git branch", e);
            return null;
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                Flog.error("Error closing git head file", e);
            }
        }
        contents = contents.replace("refs/heads/", "");
        contents = contents.replace("ref:", "");
        contents = contents.trim();
        contents = contents.replace("\n", "");
        return contents;
    }
}
