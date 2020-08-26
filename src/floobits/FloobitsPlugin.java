package floobits;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import floobits.impl.ContextImpl;
import floobits.utilities.Flog;

@Service
public class FloobitsPlugin {
    public final static String name = "Floobits-Plugin";
    public final ContextImpl context;

    public FloobitsPlugin(Project project) {
        context = new ContextImpl(project);
        Flog.info("Floobits plugin");
        FloobitsApplicationService floobitsApplicationService = ServiceManager.getService(FloobitsApplicationService.class);
        floobitsApplicationService.projectOpened(context);
        context.loadFloobitsWindow();
    }
}
