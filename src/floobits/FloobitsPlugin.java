package floobits;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;

import floobits.impl.ContextImpl;
import floobits.utilities.Flog;
import org.jetbrains.annotations.NotNull;

@Service
public class FloobitsPlugin {
    public final static String name = "Floobits-Plugin";
    public final ContextImpl context;

    public FloobitsPlugin(Project project) {
        context = new ContextImpl(project);
        Flog.info("Floobits plugin");
    }

    public void setupAccount(@NotNull Runnable afterSetup) {
        FloobitsApplicationService floobitsApplicationService = ServiceManager.getService(FloobitsApplicationService.class);
        floobitsApplicationService.setupAccount(context, afterSetup);
        context.loadFloobitsWindow();
    }
}
