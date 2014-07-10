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
import floobits.impl.ImpContext;
import floobits.utilities.Flog;
import floobits.utilities.SelectFolder;
import floobits.utilities.ThreadSafe;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.net.URI;
import java.util.Set;

public class FloobitsApplication implements ApplicationComponent {
    public static FloobitsApplication self;
    private Boolean createAccount = true;

    public FloobitsApplication() {
        self = this;
    }

    public void initComponent() {
        Migrations.migrateFloorc();
        FloorcJson floorcJson = null;
        try {
            floorcJson = Settings.get();
        } catch (Throwable e) {
            Flog.warn(e);
        }
        Set<String> strings = floorcJson != null ? floorcJson.auth.keySet() : null;
        if ((strings != null ? strings.size() : 0) == 1) {
            Constants.defaultHost = (String) strings.toArray()[0];
        }
        if (Settings.canFloobits()) {
          createAccount = false;
        }
        ApplicationInfo instance = ApplicationInfo.getInstance();
        IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId("com.floobits.unique.plugin.id"));
        String version = plugin != null ? plugin.getVersion() : "???";
        String userAgent = String.format("%s-%s-%s %s (%s-%s)", instance.getVersionName(), instance.getMajorVersion(), instance.getMinorVersion(), version, System.getProperty("os.name"), System.getProperty("os.version"));
        CrashDump.setUA(userAgent, instance.getVersionName());
    }

    public void disposeComponent() {
        // TODO: insert component disposal logic here
    }

    public synchronized void projectOpened(ImpContext context) {
        if (!createAccount) {
            return;
        }
        PersistentJson p = PersistentJson.getInstance();
        if (p.disable_account_creation) {
            context.statusMessage("Please create a Floobits account and/or make a ~/.floorc.json (https://floobits.com/help/floorc)");
            return;
        }
        createAccount = false;
        CreateAccount createAccount = new CreateAccount(context.project);
        createAccount.createCenterPanel();
        createAccount.show();
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
                final ImpContext context = FloobitsPlugin.getInstance(projectForPath).context;
                ThreadSafe.write(context, new Runnable() {
                    @Override
                    public void run() {
                        context.project.save();
                        Window window = WindowManager.getInstance().suggestParentWindow(context.project);
                        if (window != null) {
                            window.toFront();
                        }
                        context.joinWorkspace(f, location, false);
                    }
                });
            }
        });

    }

    public void joinWorkspace(ImpContext context, final FlooUrl flooUrl, final String location) {
//        if (!API.workspaceExists(flooUrl, context)) {
//            context.errorMessage(String.format("The workspace %s does not exist!", flooUrl.toString()));
//            return;
//        }
        Project projectForPath = getProject(location);

        if (context == null || projectForPath != context.project) {
            if (projectForPath == null) {
                Flog.errorMessage("The editor could not open the project :(", null);
                return;
            }
            context = FloobitsPlugin.getInstance(projectForPath).context;
        }
        // not gonna work here
        final ImpContext finalContext = context;
        ThreadSafe.write(context, new Runnable() {
            @Override
            public void run() {
                Window window = WindowManager.getInstance().suggestParentWindow(finalContext.project);
                if (window != null) {
                    window.toFront();
                }
                finalContext.joinWorkspace(flooUrl, location, false);
            }
        });
    }

    public void joinWorkspace(final ImpContext context, final String url) {
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
                openedProject = ProjectManager.getInstance().loadAndOpenProject(path);
            } catch (Throwable e) {
                Flog.warn(e);
                API.uploadCrash(null, null, e);
                return null;
            }
        }
        // This is something Intellij does when a user opens a project from the menu:
        FileChooserUtil.setLastOpenedFile(openedProject, file);
        return openedProject;
    }

}