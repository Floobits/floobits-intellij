package floobits.common;

import org.apache.commons.io.FilenameUtils;

public class Constants {
    final static public String baseDir = FilenameUtils.concat(System.getProperty("user.home"), "floobits");
    final static public String shareDir = FilenameUtils.concat(baseDir, "share");
    final static public String version = "0.11";
    final static public String pluginVersion = "0.01";
    final static public String OUTBOUND_FILTER_PROXY_HOST = "proxy.floobits.com";
    final static public int OUTBOUND_FILTER_PROXY_PORT = 443;
    final static public String floobitsDomain = "floobits.com";
    static public String defaultHost = "floobits.com";
    final static public int defaultPort = 3448;
}