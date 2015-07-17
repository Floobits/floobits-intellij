package floobits.common;

import floobits.common.interfaces.IContext;
import floobits.utilities.Flog;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

/**
 * Singleton class providing open-in-browser function.
 * Defaults to using java.awt.Desktop.browse().
 * Singleton can be replaced to provide plugin-specific browse() features
 */
public class BrowserOpener {

    private static BrowserOpener singleton;

    /**
     * @return the "singleton" BrowserOpener instance
     */
    public static BrowserOpener getInstance() {
        if (null == singleton) {
            singleton = new BrowserOpener();
        }
        return singleton;
    }

    /**
     * Replace the instance of BrowserOpener.  Can be used to provide
     * plugin-specific functionality that can still be called from common code.
     * @param newInstance the new BrowserOpener instance to use
     */
    public static void replaceSingleton(BrowserOpener newInstance) {
        singleton = newInstance;
    }

    /**
     * @return Whether we are likely to be able to open a browser window
     *         on this system.
     */
    public boolean isBrowserSupported() {
        return Desktop.isDesktopSupported();
    }

    /**
     * Open the system's default browser to the specified URI
     * @param uri - The link to open
     * @param defaultLinkText - Link text for hyperlink dropped into console if opening browser fails
     * @param context - Application context so that we can write to console if needed
     * @return boolean true if the browser was successfully opened.
     */
    public boolean openInBrowser(URI uri, String defaultLinkText, IContext context) {
        boolean shown = false;
        String linkText = "Please click here to continue.";
        if (defaultLinkText != null) {
            linkText = defaultLinkText;
        }
        if(Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(uri);
                shown = true;
            } catch (IOException error) {
                Flog.error(error);
            }
        }
        if (!shown && context != null) {
            context.errorMessage("Could not open your system's browser.");
            context.statusMessage(Utils.getLinkHTML(linkText, uri.toString()));
        }
        return shown;
    }
}
