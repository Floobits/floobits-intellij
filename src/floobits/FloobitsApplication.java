package floobits;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import floobits.common.*;
import floobits.dialogs.CreateAccount;
import floobits.utilities.Flog;
import floobits.utilities.SelectFolder;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.util.Map;

/**
 * Created by kans on 2/6/14.
 */
public class FloobitsApplication implements ApplicationComponent {
    public boolean canMakeAccount = true;
    public static FloobitsApplication self;

    public FloobitsApplication() {
        self = this;
    }

    public void initComponent() {
    }

    public void disposeComponent() {
        // TODO: insert component disposal logic here
    }

    public synchronized void projectOpened(FlooContext context) {
        if (canMakeAccount && !new Settings(context).isComplete()) {
            canMakeAccount = false;
            new CreateAccount(context);
        }
    }

    @NotNull
    public String getComponentName() {
        return "FloobitsApplication";
    }

    public void joinWorkspace(FlooContext context) {
        final String project_path = context.project.getBasePath();

        FlooUrl flooUrl = DotFloo.read(project_path);
        if (API.workspaceExists(flooUrl, context)) {
            joinWorkspace(context, flooUrl, project_path);
            return;
        }

        PersistentJson persistentJson = PersistentJson.getInstance();
        for (Map.Entry<String, Map<String, Workspace>> i : persistentJson.workspaces.entrySet()) {
            Map<String, Workspace> workspaces = i.getValue();
            for (Map.Entry<String, Workspace> j : workspaces.entrySet()) {
                Workspace w = j.getValue();
                if (Utils.isSamePath(w.path, project_path)) {
                    try {
                        flooUrl = new FlooUrl(w.url);
                    } catch (MalformedURLException e) {
                        Flog.warn(e);
                        continue;
                    }
                    if (API.workspaceExists(flooUrl, context)) {
                        joinWorkspace(context, flooUrl, w.path);
                        return;
                    }
                }
            }
        }

        context.error_message("Could not find a workspace for the current project.");
    }

    public void joinWorkspace(FlooContext context, final FlooUrl flooUrl, String location) {
//        if (!API.workspaceExists(f, context)) {
//            context.error_message(String.format("The workspace %s does not exist!", f.toString()));
//            return;
//        }
        Project projectForPath = getProject(location);
        if (projectForPath == null) {
            if (context.isJoined()) {
//                Todo ask user aboot this or something
                projectForPath = createProject(location, flooUrl.workspace);
                context = FloobitsPlugin.getInstance(projectForPath).context;
                context.joinWorkspace(flooUrl);
                return;
            } else {
                context.joinWorkspace(flooUrl);
            }
        }

        if (projectForPath != context.project) {
            context = FloobitsPlugin.getInstance(projectForPath).context;
            context.joinWorkspace(flooUrl);
            return;
        }

        context.joinWorkspace(flooUrl);
    }

    public void joinWorkspace(final FlooContext context, final String url) {
        final FlooUrl f;
        try {
            f = new FlooUrl(url);
        } catch (Exception e) {
            context.error_message(String.format("Invalid url: %s", e));
            return;
        }

        PersistentJson persistentJson = PersistentJson.getInstance();
        Workspace workspace;
        try {
            workspace = persistentJson.workspaces.get(f.owner).get(f.workspace);
        } catch (Exception e) {
            workspace = null;
        }
        if (workspace != null) {
            joinWorkspace(context, f, workspace.path);
            return;
        }
        SelectFolder.build(new RunLater<String>() {
            @Override
            public void run(String path) {
                joinWorkspace(context, f, path);
            }
        });
    }

    private Project getProject(String path) {
        ProjectManager pm = ProjectManager.getInstance();
        // Check open projects
        Project[] openProjects = pm.getOpenProjects();
        for (Project project : openProjects) {
            if (path.equals(project.getBasePath())) {
                return project;
            }
        }

        // Try to open existing project
        try {
            return pm.loadAndOpenProject(path);
        } catch (Exception e) {
            Flog.warn(e);
        }

        return null;
    }
    private Project createProject(String path, String name) {
        ProjectManager pm = ProjectManager.getInstance();
        // Create project
        Project project = pm.createProject(name, path);
        try {
            ProjectManager.getInstance().loadAndOpenProject(path);
        } catch (Exception e) {
            Flog.warn(e);
        }
        return project;
    }
}
