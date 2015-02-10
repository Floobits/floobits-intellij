package floobits.common;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

public class CrashDump implements Serializable {
    public String owner;
    public String workspace;
    public String dir;
    public String subject;
    public String username;
    public static String userAgent;
    public HashMap<String, String> message = new HashMap<String, String>();
    private static String editor = "";

    public static void setUA(String userAgent, String editor) {
        CrashDump.userAgent = userAgent;
        CrashDump.editor = editor;
    }

    private IdeaPluginDescriptor getPlugin() {
        return PluginManager.getPlugin(PluginId.getId("com.floobits.unique.plugin.id"));
    }

    public CrashDump(Throwable e, String owner, String workspace, String dir, String username) {
        this.owner = owner;
        this.workspace = workspace;
        this.dir = dir;
        this.username = username;
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        addContextInfo(message);
        message.put("stack", sw.toString());
        message.put("description", e.getMessage());
        setContextInfo("%s died%s!");
    }

    public CrashDump(String description, String username) {
        this.username = username;
        addContextInfo(message);
        message.put("description", description);
        setContextInfo("%s submitted an issues%s!");
    }

    protected void setContextInfo(String subjectText) {
        subject = String.format(subjectText, editor, username != null ? " for " + username : "");
    }

    private void addContextInfo(HashMap<String, String> message) {
        IdeaPluginDescriptor p = getPlugin();
        if (p != null) {
            message.put("floobits_plugin_version", p.getVersion());
        } else {
            message.put("floobits_plugin_version", "No version information.");
        }
        message.put("sendingAt", String.format("%s", new Date().getTime()));
        Properties props = System.getProperties();
        message.put("OS", String.format("name: %s arch: %s version: %s", props.getProperty("os.name"),
                props.getProperty("os.arch"), props.getProperty("os.version")));
        message.put("cwd", props.getProperty("user.dir"));
        message.put("file_separator", props.getProperty("file.separator"));
        message.put("path_separator", props.getProperty("path.separator"));
        message.put("line_separator", props.getProperty("line.separator"));
        message.put("java_version", props.getProperty("java.version"));
        message.put("java_vendor", props.getProperty("java.vendor"));
    }
}
