package floobits.dialogs;

import com.intellij.openapi.components.ServiceManager;
import floobits.FloobitsApplicationService;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class JoinWorkspaceDialog extends CustomButtonDialogWrapper {
    protected JoinWorkspaceByURLForm joinWorkspaceByURLForm = new JoinWorkspaceByURLForm();
    public JoinWorkspaceDialog(String url) {
        super(true);
        setTitle("Join a Floobits Workspace");
        joinWorkspaceByURLForm.setURL(url);
        CustomButtonAction cancelAction = new CustomButtonAction("Cancel", new Runnable() {
            @Override
            public void run() {}
        });
        CustomButtonAction joinAction = new CustomButtonAction("Join", new Runnable() {
            @Override
            public void run() {

                String inputValue = joinWorkspaceByURLForm.getURL();
                if (inputValue == null) {
                    return;
                }
                FloobitsApplicationService applicationService = ServiceManager.getService(FloobitsApplicationService.class);
                applicationService.joinWorkspace(inputValue);
            }
        });
        joinAction.putValue(FOCUSED_ACTION, true);
        joinAction.putValue(DEFAULT_ACTION, true);
        actions = new Action[]{
                cancelAction,
                joinAction,
        };
        init();
    }

    @Nullable
    @Override
    public JComponent createCenterPanel() {
        return joinWorkspaceByURLForm.getContentPanel();

    }
}
