package floobits.dialogs;

import javax.swing.*;

public class HandlePermissionRequestForm {
    private JLabel formLabel;
    private JPanel contentPanel;

    public void setFormLabel(String label) {
        formLabel.setText(label);
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }
}
