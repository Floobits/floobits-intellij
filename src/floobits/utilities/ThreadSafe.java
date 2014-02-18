package floobits.utilities;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import floobits.FlooContext;
import floobits.FloobitsPlugin;
import floobits.handlers.FlooHandler;

public class ThreadSafe {
    public static void write(final FlooContext context, final Runnable runnable) {
		ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                CommandProcessor.getInstance().executeCommand(context.project, new Runnable() {
                    @Override
                    public void run() {
                        ApplicationManager.getApplication().runWriteAction(runnable);
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
