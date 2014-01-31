package floobits.utilities;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import floobits.FloobitsPlugin;
import floobits.handlers.FlooHandler;

public class ThreadSafe {
    public static void write(final Runnable runnable) {
			ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                FlooHandler instance = FloobitsPlugin.getHandler();
                if (instance == null) {
                    return;
                }
                CommandProcessor.getInstance().executeCommand(instance.project, new Runnable() {
                    @Override
                    public void run() {
                        ApplicationManager.getApplication().runWriteAction(runnable);
                    }
                }, "Floobits", null);
            }
        });
    }
    public static void read(Runnable runnable) {
        ApplicationManager.getApplication().runReadAction(runnable);
    }
}
