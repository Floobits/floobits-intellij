package floobits;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.swing.event.*;
import java.awt.*;

class SelectRecentWorkspaceDialog extends DialogWrapper {
    private SelectWorkspace selectWorkspace = new SelectWorkspace();

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return selectWorkspace.getPanel();
    }

    SelectRecentWorkspaceDialog(final List<String> items) {
        super(true);
        this.setTitle("Select a Workspace");

        selectWorkspace.setItems(items);
        init();

    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        FloobitsPlugin.joinWorkspace(selectWorkspace.getSelectedItem());
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