package floobits.utilities;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import floobits.FlooContext;

public class ThreadSafe {
    public static void write(final FlooContext context, final Runnable runnable) {
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
                                Flog.log("spent %s getting lock", System.currentTimeMillis() - l);
                                runnable.run();
                            }
                        });
                    }
                }, "Floobits", null);
            }
        });
    }
    public static void read(final Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runReadAction(runnable);
            }
        }
        );
    }
}
