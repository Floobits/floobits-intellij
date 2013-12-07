package floobits;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.util.List;

public class SelectOwner extends DialogWrapper {
    private JPanel jPanel;
    private RunLater runLater;
    private String owner;

    public static void build(final List<String> title, final RunLater runLater) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                SelectOwner SelectOwner = new SelectOwner(title, runLater);
                SelectOwner.createCenterPanel();
                SelectOwner.show();
            }
        });
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return jPanel;
    }

    protected SelectOwner(List<String> title, RunLater runLater) {
        super(true);
        jPanel = new JPanel();
        this.runLater = runLater;
        init();
        this.setTitle("Select Workspace Owner");

        final JComboBox orgList = new JComboBox(title.toArray());
        // petList.setSelectedIndex(4);
        orgList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                owner = (String)orgList.getSelectedItem();
            }
        });

        JLabel jLabel = new JLabel();
        jLabel.setText("Select the owner for the workspace:");
        jPanel.add(jLabel);
        jPanel.add(orgList);
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
        // runLater.run(false);
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        runLater.run(owner);
    }
}
