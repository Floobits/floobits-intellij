package floobits.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import floobits.FloobitsPlugin;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class SelectRecentWorkspace extends DialogWrapper {
    Project project;
    private SelectWorkspace selectWorkspace = new SelectWorkspace();

    public SelectRecentWorkspace(Project project, final List<String> items) {
        super(project, true);
        this.project = project;
        setTitle("Select a Workspace");
        selectWorkspace.setItems(items);
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
        project.getComponent(FloobitsPlugin.class).joinWorkspace(selectWorkspace.getSelectedItem());
    }
}
