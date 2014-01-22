package floobits;

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
    }

    public JPanel getPanel() {
        return contentPane;
    }

    public String getSelectedItem() {
        return (String) recentWorkspaces.getSelectedValue();
    }


    public static void main(String[] args) {
        SelectWorkspace dialog = new SelectWorkspace();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
