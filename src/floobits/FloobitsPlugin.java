package floobits;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import floobits.impl.ContextImpl;
import floobits.utilities.Flog;
import org.jetbrains.annotations.NotNull;

public class FloobitsPlugin implements ProjectComponent {
    public final static String name = "Floobits-Plugin";
    public final ContextImpl context;

    public static FloobitsPlugin getInstance(Project project) {
        if (project == null) {
            return null;
        }
        return project.getComponent(FloobitsPlugin.class);
    }
    public FloobitsPlugin(Project project) {
        context = new ContextImpl(project);
        Flog.info("Floobits plugin");
    }

    @Override
    public void projectOpened() {
        FloobitsApplication.self.projectOpened(context);
        context.loadChatManager();
    }

    @Override
    public void projectClosed() {
        context.shutdown();
    }

    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {
    }

    @NotNull
    @Override
    public String getComponentName() {
        return name;
    }
}
