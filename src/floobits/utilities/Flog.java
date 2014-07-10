package floobits.utilities;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.JBColor;

import javax.swing.*;

/**
 * Do not add a Log.error statement to this class. Error statements are user visible exceptions. Use
 * Utitls.errorMessage to notify the user of a problem and Flog.warn to log an exception in a way that doesn't
 * disturb the user.
 */
public class Flog {
    public static Logger Log = Logger.getInstance(Flog.class);
    public static void log (String s, Object... args) {
        Log.info(String.format(s, args));
    }
    public static void debug (String s, Object... args) {
        Log.debug(String.format(s, args));
    }
    public static void warn (Throwable e) {
        Log.warn(e);
    }
    public static void warn (String s, Object... args) {
        Log.warn(String.format(s, args));
    }
    public static void info (String s, Object... args) {
        Log.info(String.format(s, args));
    }

    public static void statusMessage(final String message, final NotificationType notificationType, final Project project) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    public void run() {
                        Notifications.Bus.notify(new Notification("Floobits", "Floobits", message, notificationType), project);
                    }
                });
            }
        });
    }

    public static void statusMessage(String message, Project project) {
        statusMessage(message, NotificationType.INFORMATION, project);
    }

    public static void errorMessage(String message, Project project) {
        statusMessage(message, NotificationType.ERROR, project);
    }

    public static void flashMessage(final String message, final Project project) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    public void run() {
                        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
                        if (statusBar == null) {
                            return;
                        }
                        JLabel jLabel = new JLabel(message);
                        statusBar.fireNotificationPopup(jLabel, JBColor.WHITE);
                    }
                });
            }
        });
    }
}
