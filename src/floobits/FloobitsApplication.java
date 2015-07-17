package floobits;

import com.intellij.ide.impl.ProjectUtil;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileChooser.impl.FileChooserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.platform.PlatformProjectOpenProcessor;
import com.intellij.projectImport.ProjectAttachProcessor;
import floobits.common.*;
import floobits.dialogs.CreateAccount;
import floobits.impl.ContextImpl;
import floobits.utilities.Flog;
import floobits.utilities.IntelliBrowserOpener;
import floobits.utilities.SelectFolder;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.net.URI;

public class FloobitsApplication implements ApplicationComponent {
    public static FloobitsApplication self;
    private Boolean createAccount = true;

    public FloobitsApplication() {
        self = this;
    }

    public void initComponent() {
        BrowserOpener.replaceSingleton(new IntelliBrowserOpener());
        ApplicationInfo instance = ApplicationInfo.getInstance();
        IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId("com.floobits.unique.plugin.id"));
        createAccount = Bootstrap.bootstrap(instance.getVersionName(), instance.getMajorVersion(), instance.getMinorVersion(), plugin != null ? plugin.getVersion() : "???");
    }

    public void disposeComponent() {
        // TODO: insert component disposal logic here
    }

    public synchronized void projectOpened(ContextImpl context) {
        if (!createAccount) {
            return;
        }
        PersistentJson p = PersistentJson.getInstance();
        if (p.disable_account_creation) {
            context.statusMessage("Please create a Floobits account and/or make a ~/.floorc.json (https://floobits.com/help/floorc)");
            return;
        }
        createAccount = false;
        CreateAccount createAccountDialog = new CreateAccount(context.project);
        createAccountDialog.createCenterPanel();
        createAccountDialog.show();
    }

    @NotNull
    public String getComponentName() {
        return "FloobitsApplication";
    }

    public void joinWorkspace(final String url) {
        final FlooUrl f;
        try {
            f = new FlooUrl(url);
        } catch (Exception e) {
            Flog.errorMessage(String.format("Invalid url: %s", e), null);
            return;
        }
        SelectFolder.build(f.owner, f.workspace, new RunLater<String>() {
            @Override
            public void run(final String location) {
                Project projectForPath = getProject(location);
                if (projectForPath == null) {
                    Flog.errorMessage("The editor could not open the project :(", null);
                    return;
                }
                final ContextImpl context = FloobitsPlugin.getInstance(projectForPath).context;
                context.writeThread(new Runnable() {
                    @Override
                    public void run() {
                        context.project.save();
                        Window window = WindowManager.getInstance().suggestParentWindow(context.project);
                        if (window != null) {
                            window.toFront();
                        }
                        context.joinWorkspace(f, location, false, null);
                    }
                });
            }
        });

    }

    public void joinWorkspace(ContextImpl context, final FlooUrl flooUrl, final String location) {
        Project projectForPath = getProject(location);

        if (context == null || projectForPath != context.project) {
            if (projectForPath == null) {
                Flog.errorMessage("The editor could not open the project :(", null);
                return;
            }
            context = FloobitsPlugin.getInstance(projectForPath).context;
        }
        // not gonna work here
        final ContextImpl finalContext = context;
        context.writeThread(new Runnable() {
            @Override
            public void run() {
                Window window = WindowManager.getInstance().suggestParentWindow(finalContext.project);
                if (window != null) {
                    window.toFront();
                }
                finalContext.joinWorkspace(flooUrl, location, false, null);
            }
        });
    }

    public void joinWorkspace(final ContextImpl context, final String url) {
        final FlooUrl f;

        try {
            f = new FlooUrl(url);
        } catch (Throwable e) {
            Flog.errorMessage(String.format("Invalid url: %s", e), context.project);
            return;
        }

        PersistentJson persistentJson = PersistentJson.getInstance();
        Workspace workspace;
        try {
            workspace = persistentJson.workspaces.get(f.owner).get(f.workspace);
        } catch (Throwable e) {
            workspace = null;
        }
        if (workspace != null) {
            joinWorkspace(context, f, workspace.path);
            return;
        }
        if (context != null) { // Can be null if started from quick menu.
            FlooUrl flooUrl = DotFloo.read(context.project.getBasePath());
            if (flooUrl != null) {
                URI uri = URI.create(flooUrl.toString());
                URI normalizedURL = URI.create(url);

                if (uri.getPath().equals(normalizedURL.getPath())) {
                    joinWorkspace(context, flooUrl, context.project.getBasePath());
                    return;
                }
            }
        }

        SelectFolder.build(f.owner, f.workspace, new RunLater<String>() {
            @Override
            public void run(String path) {
                joinWorkspace(context, f, path);
            }
        });
    }

    private Project getProject(String path) {
        ProjectManager pm = ProjectManager.getInstance();
        Project[] openProjects = pm.getOpenProjects();
        for (Project project : openProjects) {
            if (path.equals(project.getBasePath())) {
                return project;
            }
        }
        VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(new File(path));
        Project openedProject;
        if (ProjectAttachProcessor.canAttachToProject() && file != null) {
            openedProject = PlatformProjectOpenProcessor.doOpenProject(file, null, false, -1, null, false);
        } else {
            openedProject = ProjectUtil.openOrImport(path, null, false);
        }
        if (openedProject == null) {
            try {
                String projectFilePath = ".idea/misc.xml";
                if (path.endsWith(projectFilePath)) {
                    Flog.error("Attempted to open the project misc.xml file instead of the directory.");
                    path = path.replace(projectFilePath, "");
                }
                openedProject = ProjectManager.getInstance().loadAndOpenProject(path);
            } catch (Throwable e) {
                Flog.error(e);
                API.uploadCrash(null, null, e);
                return null;
            }
        }
        // This is something Intellij does when a user opens a project from the menu:
        FileChooserUtil.setLastOpenedFile(openedProject, file);
        return openedProject;
    }

}
