package floobits.utilities;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import floobits.FlooContext;
import floobits.common.API;

public class ThreadSafe {
    public static void later(final Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable);
    }

    public static void write(final FlooContext context, final Runnable runnable) {
        final long l = System.currentTimeMillis();
        later(new Runnable() {
            @Override
            public void run() {
                CommandProcessor.getInstance().executeCommand(context.project, new Runnable() {
                    @Override
                    public void run() {
                        ApplicationManager.getApplication().runWriteAction(new Runnable() {
                            @Override
                            public void run() {
                                long time = System.currentTimeMillis() - l;
                                if (time > 200) {
                                    Flog.log("spent %s getting lock", time);
                                }
                                try {
                                    runnable.run();
                                } catch (Throwable throwable) {
                                    API.uploadCrash(context, throwable);
                                }
                            }
                        });
                    }
                }, "Floobits", null);
            }
        });
    }
    public static void read(final FlooContext context, final Runnable runnable) {
        later(new Runnable() {
            @Override
            public void run() {
                try {
                    ApplicationManager.getApplication().runReadAction(runnable);
                } catch(Throwable throwable) {
                    API.uploadCrash(context, throwable);
                }
            }
        }
        );
    }
}
