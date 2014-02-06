package floobits.common;

import com.intellij.openapi.project.Project;
import org.apache.commons.io.FilenameUtils;

public class Constants {
    final static public String baseDir = FilenameUtils.concat(System.getProperty("user.home"), "floobits");
    final static public String version = "0.10";
    final static public String pluginVersion = "0.01";
    final static public String defaultHost = "floobits.com";
    final static public int defaultPort = 3448;

}