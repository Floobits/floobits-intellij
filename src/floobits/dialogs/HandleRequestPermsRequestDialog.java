package floobits.dialogs;

import com.intellij.openapi.project.Project;
import floobits.common.RunLater;
import floobits.utilities.Flog;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class HandleRequestPermsRequestDialog extends CustomButtonDialogWrapper {
    HandlePermissionRequestForm form = new HandlePermissionRequestForm();

    public HandleRequestPermsRequestDialog(String username, Project project, final RunLater<String> runLater) {
        super(project, true);
        actions = new Action[]{
            new CustomButtonAction("Reject Request", new Runnable() {
                @Override
                public void run() {
                    Flog.log("Rejecting edit permission request.");
                    runLater.run("reject");
                }
            }),
            new CustomButtonAction("Grant Edit Permissions", new Runnable() {
                @Override
                public void run() {
                    Flog.log("Accepting edit permission request.");
                    runLater.run("add");

                }
            }),
            new CustomButtonAction("Ignore", new Runnable() {
                @Override
                public void run() {
                    Flog.log("Ignoring edit permission request.");
                }
            }),
        };
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
