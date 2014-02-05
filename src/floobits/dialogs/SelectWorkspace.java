package floobits.dialogs;

import com.intellij.ui.components.JBList;

import javax.swing.*;
import java.util.List;

public class SelectWorkspace extends JDialog {
    private JPanel contentPane;
    private JBList recentWorkspaces;

    public SelectWorkspace() {
        setContentPane(contentPane);
    }

    public void setItems(final List<String> items) {
        recentWorkspaces.setListData(items.toArray());
        if (items.size() > 0) {
            recentWorkspaces.getSelectionModel().setSelectionInterval(0, 0);
        }
    }

    public JPanel getPanel() {
        return contentPane;
    }

    public String getSelectedItem() {
        return (String) recentWorkspaces.getSelectedValue();
    }

}
