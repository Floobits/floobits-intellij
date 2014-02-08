package floobits;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import floobits.utilities.Flog;
import org.jetbrains.annotations.NotNull;

public class FloobitsPlugin implements ProjectComponent {
    public final static String name = "Floobits-Plugin";
    public final FlooContext context;

    public static FloobitsPlugin getInstance(Project project) {
        return project.getComponent(FloobitsPlugin.class);
    }
    public FloobitsPlugin(Project project) {
        context = new FlooContext(project);
        Flog.info("Floobits plugin");
    }

    @Override
    public void projectOpened() {
        FloobitsApplication.self.projectOpened(context);
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
