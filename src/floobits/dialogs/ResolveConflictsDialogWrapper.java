package floobits.dialogs;

import floobits.common.RunLater;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class ResolveConflictsDialogWrapper extends CustomButtonDialogWrapper {
    protected ResolveConflictsForm form = new ResolveConflictsForm();

    public ResolveConflictsDialogWrapper(RunLater<Void> stompLocal, RunLater<Void> stompRemote, boolean readOnly, RunLater<Void> flee, final String[] conflicts) {
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
