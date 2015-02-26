package floobits.utilities;

import com.intellij.notification.*;
import com.intellij.notification.impl.NotificationsConfigurationImpl;
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
    public static String ID = "Floobits notifications";
    public static Logger Log = Logger.getInstance(Flog.class);
    private static String maybeFormat(String s, Object... args) {
        if (args.length == 0) {
            return s;
        } else {
            return String.format(s, args);
        }
    }
    public static void log (String s, Object... args) {
        Log.info(maybeFormat(s, args));
    }
    public static void debug (String s, Object... args) {
        Log.debug(maybeFormat(s, args));
    }
    public static void warn (Throwable e) {
        Log.warn(e);
    }
    public static void warn (String s, Object... args) {
        Log.warn(maybeFormat(s, args));
    }
    public static void info (String s, Object... args) {
        Log.info(maybeFormat(s, args));
    }
    private static void ensureRegistered() {
        if (!(NotificationsConfigurationImpl.getNotificationsConfigurationImpl().isRegistered(ID))) {
            NotificationsConfiguration.getNotificationsConfiguration().register(ID, NotificationDisplayType.BALLOON, false);
        }
    }
    public static void statusMessage(final String message, final NotificationType notificationType, final Project project) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    public void run() {
                        try {
                            ensureRegistered();
                            final Notification statusMessage = new Notification("Floobits", "Floobits", message, notificationType);
                            Notifications.Bus.notify(statusMessage, project);
                            boolean sticky = NotificationsConfigurationImpl.getSettings(ID).getDisplayType() == NotificationDisplayType.STICKY_BALLOON;
                            if (!sticky) {
                                java.util.Timer timer = new java.util.Timer();
                                timer.schedule(new java.util.TimerTask() {
                                    @Override
                                    public void run() {
                                        statusMessage.expire();
                                    }
                                }, 3000);
                            }
                        } catch (Throwable e) {
                            Flog.warn(e);
                            Flog.log(message);
                        }
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
