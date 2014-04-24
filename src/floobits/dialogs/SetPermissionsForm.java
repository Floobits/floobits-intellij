package floobits.dialogs;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SetPermissionsForm {
    private JPanel contentContainer;
    private JCheckBox viewCheckbox;
    private JCheckBox requestPermCheckbox;
    private JCheckBox editCheckbox;
    private JCheckBox adminCheckbox;
    private JLabel setPermissionsLabel;

    public SetPermissionsForm() {
        viewCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!viewCheckbox.isSelected()) {
                    editCheckbox.setSelected(false);
                    adminCheckbox.setSelected(false);
                }
            }
        });
        requestPermCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!requestPermCheckbox.isSelected()) {
                   editCheckbox.setSelected(false);
                   adminCheckbox.setSelected(false);
                }
            }
        });
        editCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (editCheckbox.isSelected()) {
                    viewCheckbox.setSelected(true);
                    requestPermCheckbox.setSelected(true);
                } else {
                   adminCheckbox.setSelected(false);
                }
            }
        });
        adminCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (adminCheckbox.isSelected()) {
                    viewCheckbox.setSelected(true);
                    requestPermCheckbox.setSelected(true);
                    editCheckbox.setSelected(true);
                }
            }
        });
    }

    public JPanel getContentPanel() {
        return contentContainer;
    }

    public void setUsername(String username) {
        setPermissionsLabel.setText(String.format("Edit permissions for %s:", username));
    }

    public void setPermissions(boolean view, boolean request, boolean edit, boolean admin) {
        viewCheckbox.setSelected(view);
        requestPermCheckbox.setSelected(request);
        editCheckbox.setSelected(edit);
        adminCheckbox.setSelected(admin);
    }

    public Boolean canAdmin() {
        return adminCheckbox.isSelected();
    }

    public Boolean canEdit() {
        return editCheckbox.isSelected();
    }

    public Boolean canView() {
        return viewCheckbox.isSelected();
    }

    public Boolean canRequestPermissions() {
        return requestPermCheckbox.isSelected();
    }
}
