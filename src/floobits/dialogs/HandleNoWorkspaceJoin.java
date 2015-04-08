package floobits.dialogs;

import com.intellij.openapi.project.Project;
import floobits.impl.ContextImpl;
import floobits.utilities.Flog;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class HandleNoWorkspaceJoin extends CustomButtonDialogWrapper {
    protected JPanel container;


    public HandleNoWorkspaceJoin(final ContextImpl context) {
        super(true);
        container = new JPanel();
        JLabel infoLabel = new JLabel();
        infoLabel.setText("This project doesn't seem to be associated with a Floobits workspace. Create one?");
        container.add(infoLabel);
        CustomButtonAction abortAction = new CustomButtonAction("Cancel", new Runnable() {
            @Override
            public void run() {
                context.errorMessage("This project doesn't seem to be associated with a Floobits workspace.");
                Flog.warn("User aborted joining workspace.");
            }
        });
        CustomButtonAction sharePubliclyAction = new CustomButtonAction("Share project publicly", new Runnable() {
            @Override
            public void run() {
                container.setVisible(false);
                shareProject(false, context);
            }
        });
        CustomButtonAction sharePrivatelyAction = new CustomButtonAction("Share project privately", new Runnable() {
            @Override
            public void run() {
                container.setVisible(false);
                shareProject(true, context);
            }
        });
        actions = new Action[]{sharePubliclyAction, sharePrivatelyAction, abortAction};
        init();
    }

    protected void shareProject(boolean sharePrivate, ContextImpl context) {
        Project project = context.project;
        if (project == null ) {
            return;
        }
        final String project_path = project.getBasePath();
        context.shareProject(sharePrivate, project_path);
    }

    @Nullable
    @Override
    public JComponent createCenterPanel() {
        return container;

    }
}
