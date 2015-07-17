package floobits.utilities;

import com.intellij.ide.BrowserUtil;
import floobits.common.BrowserOpener;
import floobits.common.interfaces.IContext;

import java.net.URI;

/**
 * Override openInBrowser behavior to try IntelliJ's built-in function first.
 * Must be instantiated and used to replace the default BrowserOpener singleton.
 */
public class IntelliBrowserOpener extends BrowserOpener {

    /**
     * @return Whether we are likely to be able to open a browser window
     *         on this system (IntelliJ assumes this will always work)
     */
    public boolean isBrowserSupported() {
        return true;
    }

    /**
     * Wrap the common Utils.openInBrowser function so that we first attempt to use
     * IntelliJ's native open-in-browser feature, which works more reliably across platforms.
     * @param uri - The link to open
     * @param defaultLinkText - Link text for hyperlink dropped into console if opening browser fails
     * @param context - Application context so that we can write to console if needed
     * @return boolean true if the browser was successfully opened.
     */
    public boolean openInBrowser(URI uri, String defaultLinkText, IContext context) {
        boolean shown;
        try {
            BrowserUtil.browse(uri);
            shown = true;
        } catch (Exception e) {
            BrowserOpener defaultOpener = new BrowserOpener();
            shown = defaultOpener.openInBrowser(uri, defaultLinkText, context);
        }
        return shown;
    }
}
