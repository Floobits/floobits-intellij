package floobits.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import floobits.common.RunLater;
import floobits.utilities.Flog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;

public class FollowUserDialog extends DialogWrapper {
    private RunLater<FollowUserDialog> runLater;
    protected FollowUserForm form = new FollowUserForm();

    private class CreateWorkspaceAction extends DialogWrapperAction {

        protected CreateWorkspaceAction() {
            super("Save");
        }

        @Override
        protected void doAction(ActionEvent e) {
            Flog.info("Following users.");
            doOKAction();
        }
    }

    public FollowUserDialog(HashMap<String, Boolean> usersToChoose, Project project, RunLater<FollowUserDialog> runLater) {
        super(project, true);
        this.runLater = runLater;
        init();
        form.setUsers(usersToChoose);
        this.setTitle("Follow User");
    }

    @Override
    public JComponent createCenterPanel() {
        return form.getContentPanel();
    }

    @Override
    public void createDefaultActions() {
        super.createDefaultActions();
        myOKAction = new CreateWorkspaceAction();
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        if (runLater == null) {
            return;
        }
        runLater.run(this);
    }

    public List<String> getFollowedUsers() {
        return form.getUserModel().toList();
    }
}
