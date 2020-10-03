package floobits.dialogs;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import floobits.FloobitsApplicationService;
import floobits.FloobitsPlugin;
import floobits.impl.ContextImpl;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class SelectRecentWorkspace extends DialogWrapper {
    Project project;
    private SelectWorkspace selectWorkspace = new SelectWorkspace();

    public SelectRecentWorkspace(Project project, final List<String> items) {
        super(project, true);
        this.project = project;
        setTitle("Select a Workspace");
        selectWorkspace.setItems(items);
        selectWorkspace.getRecentWorkspaces().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    doOKAction();
                }
            }
        });
        init();
    }

    @Nullable
    @Override
    public JComponent createCenterPanel() {
        return selectWorkspace.getPanel();
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        FloobitsApplicationService floobitsApplicationService = ServiceManager.getService(FloobitsApplicationService.class);
        if (project != null) {
            ContextImpl context = project.getService(FloobitsPlugin.class).context;
            floobitsApplicationService.joinWorkspace(context, selectWorkspace.getSelectedItem());
            return;
        }
        floobitsApplicationService.joinWorkspace(null, selectWorkspace.getSelectedItem());

    }
}
