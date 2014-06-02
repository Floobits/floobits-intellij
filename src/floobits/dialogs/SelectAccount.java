package floobits.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Created by kans on 6/2/14.
 */
public class SelectAccount extends DialogWrapper {
    private JComboBox cccccomboBox;
    private JPanel contentPanel;

    public SelectAccount(@Nullable Project project, String[] accounts) {
        super(project);
        setTitle("Select a Floobits Account");
        contentPanel = new JPanel();
        cccccomboBox = new ComboBox(new DefaultComboBoxModel(accounts));
        if (accounts.length > 0) {
            cccccomboBox.setSelectedIndex(0);
        }
        contentPanel.add(cccccomboBox);
        JLabel label = new JLabel("Select Account");
        contentPanel.add(label);
        init();
    }

    public String getAccount() {
        return (String) cccccomboBox.getSelectedItem();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPanel;
    }
}
