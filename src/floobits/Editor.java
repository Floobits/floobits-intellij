package floobits;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.extensions.PluginId;

/**
 * Created by kans on 7/3/14.
 */
public class Editor {
    public static String userAgent() {
        ApplicationInfo instance = ApplicationInfo.getInstance();
        IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId("com.floobits.unique.plugin.id"));
        String version = plugin != null ? plugin.getVersion() : "???";
        return String.format("%s-%s-%s %s (%s-%s)", instance.getVersionName(), instance.getMajorVersion(), instance.getMinorVersion(), version, System.getProperty("os.name"), System.getProperty("os.version"));
    }

    public static String editorName() {
        ApplicationInfo instance = ApplicationInfo.getInstance();
        return instance.getVersionName();
    }
}
