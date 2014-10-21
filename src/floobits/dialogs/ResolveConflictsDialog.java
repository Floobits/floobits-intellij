package floobits.dialogs;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class ResolveConflictsDialog extends CustomButtonDialogWrapper {
    protected FileListPromptForm form = new FileListPromptForm("The following remote %s different from your version.");

    public ResolveConflictsDialog(Runnable stompLocal, Runnable stompRemote, boolean readOnly, Runnable flee,
                                  final String[] conflicts, final String[]connections) {
        super(true);
        form.setItems(conflicts);
        form.setConnections(connections);
        CustomButtonAction stompRemoteAction = new CustomButtonAction("Overwrite Remote Files", stompRemote);
        if (readOnly) {
            stompRemoteAction.setEnabled(false);
        }
        actions = new Action[]{
                new CustomButtonAction("Overwrite Local Files", stompLocal),
                stompRemoteAction,
                new CustomButtonAction("Disconnect", flee),
        };
        init();
    }

    @Nullable
    @Override
    public JComponent createCenterPanel() {
        return form.getContentPanel();

    }
}
