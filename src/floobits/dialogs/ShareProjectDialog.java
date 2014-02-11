package floobits.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import floobits.common.Utils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class ShareProjectDialog extends DialogWrapper {
    protected ShareProjectForm form = new ShareProjectForm();

    protected ShareProjectDialog(String workspaceName, List<String> orgs, Project project) {
        super(project, true);

        if (orgs.size() < 1 && project != null) {
            Utils.error_message("Unable to share project, do you have a Floobits account?", project);
            return;
        }
        form.setWorkSpaceName(workspaceName);
        form.setOrgs(orgs);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return form.getContentPanel();
    }
}
