package floobits.dialogs;

import com.intellij.openapi.ui.DialogWrapper;
import floobits.common.RunLater;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;


public class ResolveConflictsDialogWrapper extends DialogWrapper {
    protected ResolveConflictsForm form = new ResolveConflictsForm();
    protected Action[] actions;

    protected class StompAction extends DialogWrapperAction {
        protected RunLater<Void> action;

        protected StompAction(String name, RunLater<Void> action) {
            super(name);
            this.action = action;
        }

        @Override
        protected void doAction(ActionEvent e) {
            action.run(null);
            close(OK_EXIT_CODE);
        }
    }

    public ResolveConflictsDialogWrapper(RunLater<Void> stompLocal, RunLater<Void> stompRemote, boolean readOnly, RunLater<Void> flee, final String[] conflicts) {
        super(true);
        form.setItems(conflicts);
        StompAction stompRemoteAction = new StompAction("Stomp Remote Files", stompRemote);
        if (readOnly) {
            stompRemoteAction.setEnabled(false);
        }

        actions = new Action[]{
                new StompAction("Stomp Local Files", stompLocal),
                stompRemoteAction,
                new StompAction("Disconnect", flee),
        };
        init();
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return actions;
    }

    @Nullable
    @Override
    public JComponent createCenterPanel() {
        return form.getContentPanel();

    }
}
