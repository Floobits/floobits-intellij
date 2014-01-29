package floobits;

import com.intellij.openapi.application.ApplicationManager;

public class ThreadSafe {
    public static void write(final Runnable runnable) {
			ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(runnable);
            }
        });
    }
    public static void read(Runnable runnable) {
        ApplicationManager.getApplication().runReadAction(runnable);
    }
}
