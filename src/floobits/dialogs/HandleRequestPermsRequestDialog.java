package floobits.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import floobits.common.RunLater;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class HandleRequestPermsRequestDialog extends DialogWrapper {
    HandlePermissionRequestForm form = new HandlePermissionRequestForm();

    public HandleRequestPermsRequestDialog(String username, Project project, RunLater<ShareProjectDialog> runLater) {
        super(project, true);
        String label = String.format("%s is requesting edit permissions for this workspace.", username);
        form.setFormLabel(label);
        init();
        this.setTitle(label);
    }

    @Nullable
    @Override
    public JComponent createCenterPanel() {
        return form.getContentPanel();
    }
}
