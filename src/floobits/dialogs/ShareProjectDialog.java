package floobits.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import floobits.common.RunLater;
import floobits.utilities.Flog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class ShareProjectDialog extends CustomButtonDialogWrapper {
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

    public ShareProjectDialog(String workspaceName, List<String> orgs, Project project,
                              final RunLater<ShareProjectDialog> uploadAll,
                              final RunLater<ShareProjectDialog> uploadPick) {
        super(project, true);

        if (orgs.size() < 1 && project != null) {
            Flog.errorMessage("Unable to share project, do you have a Floobits account?", project);
            return;
        }
        form.setWorkSpaceName(workspaceName);
        form.setOrgs(orgs);
        final ShareProjectDialog self = this;
        this.setTitle("Create a New Workspace");
        CustomButtonAction abortAction = new CustomButtonAction("Abort", new Runnable() {
            @Override
            public void run() {
                Flog.warn("User aborted joining workspace.");
            }
        });
        CustomButtonAction uploadProject = new CustomButtonAction("Upload Entire Project", new Runnable() {
            @Override
            public void run() {
                uploadAll.run(self);
            }
        });
        CustomButtonAction uploadFiles = new CustomButtonAction("Select Files to Upload", new Runnable() {
            @Override
            public void run() {
                uploadPick.run(self);
            }
        });
        actions = new Action[]{uploadProject, uploadFiles, abortAction};
        init();
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
}
