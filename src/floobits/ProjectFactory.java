package floobits;

import java.util.ArrayList;
import java.io.IOException;
import org.jdom.JDOMException;

import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.project.impl.ProjectManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ex.ProjectRootManagerEx;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import org.apache.commons.io.FilenameUtils;


class ProjectFactory {
    public static Project create (String projectPath, String projectName) {
        final Project prj = ProjectManagerImpl.getInstanceEx().newProject(projectName, projectPath, true, false);
        Sdk [] jdks = ProjectJdkTable.getInstance().getAllJdks();
        final Sdk projectjdk = jdks[0];

        if (projectjdk != null) {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                        public void run () {
                            ProjectRootManagerEx.getInstanceEx(prj).setProjectSdk(projectjdk);
                        }
                    });
                }
            });
        }

        prj.save();

        final JavaModuleBuilder modulebuilder = new JavaModuleBuilder();
        final ModifiableModuleModel model = ModuleManager.getInstance(prj).getModifiableModel();
        modulebuilder.setName(projectName);

        modulebuilder.setModuleFilePath(FilenameUtils.concat(projectPath, projectName + ".iml"));
        ArrayList list = new ArrayList();
        list.add(new Pair(projectPath, ""));
        modulebuilder.setSourcePaths(list);
        String classesPath = FilenameUtils.concat(projectPath, "classes");
        modulebuilder.setCompilerOutputPath(classesPath);
        modulebuilder.setContentEntryPath(projectPath);

        if (model != null) {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                        public void run () {
                            try {
                                modulebuilder.createModule(model);
                                modulebuilder.setModuleJdk(projectjdk);
                                model.commit();
                            } catch (InvalidDataException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (ModuleWithNameAlreadyExists moduleWithNameAlreadyExists) {
                                moduleWithNameAlreadyExists.printStackTrace();
                            } catch (JDOMException e) {
                                e.printStackTrace();
                            } catch (ConfigurationException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                };
            });
        }

        StartupManager.getInstance(prj).registerPostStartupActivity(new Runnable() {
            public void run() {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    public void run () {
                        ToolWindow toolwindow = ToolWindowManager.getInstance(prj).getToolWindow(ToolWindowId.PROJECT_VIEW);
                        if (toolwindow != null) {
                            toolwindow.activate(null);
                        }
                        if (modulebuilder == null) {
                            ModulesConfigurator.showDialog(prj, null, null);
                        }
                    }
                });
            }
        });

        ProjectManagerEx.getInstanceEx().openProject(prj);
        return prj;
    }
}
