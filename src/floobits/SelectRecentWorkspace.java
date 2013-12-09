package floobits;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.util.LinkedList;
import java.util.List;
import javax.swing.event.*;
import java.awt.*;

class SelectRecentWorkspaceDialog extends DialogWrapper {
    private JPanel jPanel;
    private String selection;

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return jPanel;
    }

    SelectRecentWorkspaceDialog(final List<String> items) {
        super(true);
        jPanel = new JPanel();
        init();
        this.setTitle("Select Workspace");

        JList list = new JBList(items);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                selection = items.get(listSelectionEvent.getFirstIndex());

            }
        });
        JPanel listContainer = new JPanel(new GridLayout(1,1));
        listContainer.setBorder(BorderFactory.createTitledBorder(
                "Recent Workspaces"));
        listContainer.add(list);
        jPanel.add(listContainer);
        jPanel.add(list);
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
        // runLater.run(false);
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        FloobitsPlugin.joinWorkspace(selection);
    }
}

public class SelectRecentWorkspace extends AnAction {

    public void actionPerformed(AnActionEvent e) {
        PersistentJson persistentJson = PersistentJson.getInstance();
        LinkedList<String> recent = new LinkedList<String>();
        for (Workspace workspace : persistentJson.recent_workspaces) {
            recent.add(workspace.url);
        }
        SelectRecentWorkspaceDialog selectRecentWorkspaceDialog = new SelectRecentWorkspaceDialog(recent);
        selectRecentWorkspaceDialog.createCenterPanel();
        selectRecentWorkspaceDialog.show();
    }
}