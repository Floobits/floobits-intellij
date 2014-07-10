package floobits.utilities;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import floobits.common.interfaces.FlooContext;
import floobits.common.API;
import floobits.impl.IntelliContext;

public class ThreadSafe {
    public static void later(final Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable);
    }

    public static void write(FlooContext context, final Runnable runnable) {
        final IntelliContext context1 = (IntelliContext)context;
        final long l = System.currentTimeMillis();
        later(new Runnable() {
            @Override
            public void run() {
                CommandProcessor.getInstance().executeCommand(context1!= null ? context1.project : null, new Runnable() {
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
                                    API.uploadCrash(context1, throwable);
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
