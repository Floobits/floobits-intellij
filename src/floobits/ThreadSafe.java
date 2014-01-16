package floobits;

import com.intellij.openapi.application.ApplicationManager;

class ThreadSafe {
    static void write (final Runnable runnable) {
			ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(runnable);
            }
        });
    }
    static void read (Runnable runnable) {
        ApplicationManager.getApplication().runReadAction(runnable);
    }
}
