package floobits;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;

public class FloobitsProjectListener implements ProjectManagerListener {
    @Override
    public void projectOpened(@NotNull Project project) {
        FloobitsPlugin floobitsPlugin = ServiceManager.getService(FloobitsPlugin.class);
    }

    @Override
    public void projectClosed(@NotNull Project project) {
        FloobitsPlugin floobitsPlugin = ServiceManager.getServiceIfCreated(FloobitsPlugin.class);
        if (floobitsPlugin != null) {
            floobitsPlugin.context.shutdown();
        }
    }
}
