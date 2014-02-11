package floobits.dialogs;

import javax.swing.*;
import java.util.List;

/**
 * User: bjorn
 * Date: 2/11/14
 * Time: 12:23 PM
 */
public class ShareProjectForm {
    private JComboBox orgsComboBox;
    private JTextField workspaceNameTextField;
    private JPanel contentPanel;


    public void setWorkSpaceName(String workspaceName) {
        workspaceNameTextField.setText(workspaceName);
    }

    public void setOrgs(List<String> orgs) {
        orgsComboBox.setModel(new DefaultComboBoxModel(orgs.toArray()));
        orgsComboBox.setSelectedIndex(0);
    }

    public JComponent getContentPanel() {
        return contentPanel;
    }
}
