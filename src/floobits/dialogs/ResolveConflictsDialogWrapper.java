package floobits.dialogs;

import com.intellij.openapi.ui.DialogWrapper;
import floobits.common.RunLater;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;


public class ResolveConflictsDialogWrapper extends DialogWrapper {
    protected RunLater<Void> stompLocal;
    protected RunLater<Void> stompRemote;
    protected RunLater<Void> flee;
    protected ResolveConflictsForm form = new ResolveConflictsForm();

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

    protected class StompLocalAction extends StompAction {

        protected StompLocalAction(RunLater<Void> action) {
            super("Stomp Local Files", action);
        }
    }
    protected class StompRemoteAction extends StompAction {

        protected StompRemoteAction(RunLater<Void> action) {
            super("Stomp Remote Files", action);
        }
    }
    protected class FleeWorkspaceAction extends StompAction {

        protected FleeWorkspaceAction(RunLater<Void> action) {
            super("Disconnect", action);
        }
    }



    public ResolveConflictsDialogWrapper(RunLater<Void> stompLocal, RunLater<Void> stompRemote, RunLater<Void> flee, final String[] conflicts) {
        super(true);
        this.stompLocal = stompLocal;
        this.stompRemote = stompRemote;
        this.flee = flee;
        form.setItems(conflicts);
        init();
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[]{
                new StompLocalAction(stompLocal),
                new StompRemoteAction(stompRemote),
                new FleeWorkspaceAction(flee),
        };
    }

    @Nullable
    @Override
    public JComponent createCenterPanel() {
        return form.getContentPanel();
    }
}
