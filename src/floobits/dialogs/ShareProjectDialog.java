package floobits.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import floobits.common.RunLater;
import floobits.common.Utils;
import floobits.utilities.Flog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class ShareProjectDialog extends DialogWrapper {
    private RunLater<ShareProjectDialog> runLater;
    protected ShareProjectForm form = new ShareProjectForm();



    private class CreateWorkspaceAction extends DialogWrapper.DialogWrapperAction {

        protected CreateWorkspaceAction() {
            super("Create Workspace");
        }

        @Override
        protected void doAction(ActionEvent e) {
            Flog.info("Creating a workspace from project.");
            doOKAction();
        }
    }

    public ShareProjectDialog(String workspaceName, List<String> orgs, Project project, RunLater<ShareProjectDialog> runLater) {
        super(project, true);

        if (orgs.size() < 1 && project != null) {
            Utils.error_message("Unable to share project, do you have a Floobits account?", project);
            return;
        }
        this.runLater = runLater;
        form.setWorkSpaceName(workspaceName);
        form.setOrgs(orgs);
        init();
        this.setTitle("Create a New Workspace");
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

    public String getWorkspaceName() {
        return form.getWorkspaceName();
    }

    public String getOrgName() {
        return form.getSelectedOrg();
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        if (runLater == null) {
            return;
        }
        runLater.run(this);
    }
}
