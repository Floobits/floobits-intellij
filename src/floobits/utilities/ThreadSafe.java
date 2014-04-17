package floobits.utilities;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import floobits.BaseContext;
import floobits.common.API;
import floobits.common.handlers.FlooHandler;

public class ThreadSafe {
    public static void write(final BaseContext context, final Runnable runnable) {
        final long l = System.currentTimeMillis();
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                CommandProcessor.getInstance().executeCommand(context.project, new Runnable() {
                    @Override
                    public void run() {
                        ApplicationManager.getApplication().runWriteAction(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Flog.log("spent %s getting lock", System.currentTimeMillis() - l);
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
    public static void read(final BaseContext context, final Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
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
