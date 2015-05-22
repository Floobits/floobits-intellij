package floobits.dialogs;

import javax.swing.*;

public class JoinWorkspaceByURLForm {
    private JTextField urlTextField;
    private JPanel container;

    public void setURL(String url) {
        urlTextField.setText(url);
        urlTextField.selectAll();
    }

    public String getURL() {
        return urlTextField.getText();
    }

    public JComponent getContentPanel() {
        return container;
    }
}
