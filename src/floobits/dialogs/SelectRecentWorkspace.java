package floobits.dialogs;

import com.intellij.openapi.ui.DialogWrapper;
import floobits.FloobitsPlugin;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class SelectRecentWorkspace extends DialogWrapper {
    private SelectWorkspace selectWorkspace = new SelectWorkspace();

    @Nullable
    @Override
    public JComponent createCenterPanel() {
        return selectWorkspace.getPanel();
    }

    public SelectRecentWorkspace(final List<String> items) {
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
