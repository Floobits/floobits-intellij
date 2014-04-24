package floobits.dialogs;

import floobits.common.RunLater;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class SetPermissionsDialog extends CustomButtonDialogWrapper {
    protected SetPermissionsForm form = new SetPermissionsForm();

    public SetPermissionsDialog(final RunLater<String[]> runLater, boolean view, boolean request, boolean edit, boolean admin) {
        super(true);
        setTitle("Edit Permissions");
        form.setPermissions(view, request, edit, admin);
        actions = new Action[]{
            new CustomButtonAction("Cancel", null),
            new CustomButtonAction("Change permissions", new RunLater<Void>() {
                @Override
                public void run(Void arg) {
                    List<String> permissions = new ArrayList<String>();
                    if (form.canAdmin()) {
                        permissions.add("admin_room");
                    }
                    if (form.canEdit()) {
                        permissions.add("edit_room");
                    }
                    if (form.canRequestPermissions()) {
                        permissions.add("request_perms");
                    }
                    if (form.canView()) {
                        permissions.add("view_room");
                    }
                    runLater.run(permissions.toArray(new String[permissions.size()]));
                }
            }),
        };
        init();
    }

    public void setUsername(String username) {
        form.setUsername(username);
    }

    @Nullable
    @Override
    public JComponent createCenterPanel() {
        return form.getContentPanel();

    }
}
