package floobits.dialogs;

import floobits.common.RunLater;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class ResolveConflictsDialog extends CustomButtonDialogWrapper {
    protected FileListPromptForm form = new FileListPromptForm("The following remote %s different from your version.");

    public ResolveConflictsDialog(RunLater<Void> stompLocal, RunLater<Void> stompRemote, boolean readOnly, RunLater<Void> flee, final String[] conflicts) {
        super(true);
        form.setItems(conflicts);
        CustomButtonAction stompRemoteAction = new CustomButtonAction("Stomp Remote Files", stompRemote);
        if (readOnly) {
            stompRemoteAction.setEnabled(false);
        }
        actions = new Action[]{
                new CustomButtonAction("Stomp Local Files", stompLocal),
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
