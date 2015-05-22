package floobits.dialogs;

import floobits.FloobitsApplication;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class JoinWorkspaceDialog extends CustomButtonDialogWrapper {
    protected JoinWorkspaceByURLForm joinWorkspaceByURLForm = new JoinWorkspaceByURLForm();
    public JoinWorkspaceDialog(String url) {
        super(true);
        setTitle("Join a Floobits Workspace");
        joinWorkspaceByURLForm.setURL(url);
        actions = new Action[]{
                new CustomButtonAction("Join", new Runnable() {
                    @Override
                    public void run() {

                        String inputValue = joinWorkspaceByURLForm.getURL();
                        if (inputValue == null) {
                            return;
                        }
                        FloobitsApplication.self.joinWorkspace(inputValue);
                    }
                }),
        };
        init();
    }

    @Nullable
    @Override
    public JComponent createCenterPanel() {
        return joinWorkspaceByURLForm.getContentPanel();

    }
}
