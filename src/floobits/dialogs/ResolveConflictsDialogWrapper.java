package floobits.dialogs;

import com.intellij.openapi.ui.DialogWrapper;
import floobits.common.RunLater;
import floobits.utilities.Flog;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;


public class ResolveConflictsDialogWrapper extends DialogWrapper {
    protected RunLater stompLocal;
    protected RunLater stompRemote;
    protected RunLater flee;
    protected ResolveConflictsForm form = new ResolveConflictsForm();

    protected class StompAction extends DialogWrapperAction {
        protected RunLater action;

        protected StompAction(String name, RunLater action) {
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

        protected StompLocalAction(RunLater action) {
            super("Stomp Local Files", action);
        }
    }
    protected class StompRemoteAction extends StompAction {

        protected StompRemoteAction(RunLater action) {
            super("Stomp Remote Files", action);
        }
    }
    protected class FleeWorkspaceAction extends StompAction {

        protected FleeWorkspaceAction(RunLater action) {
            super("Disconnect", action);
        }
    }



    public ResolveConflictsDialogWrapper(RunLater stompLocal, RunLater stompRemote, RunLater flee, final String[] conflicts) {
        super(true);
        this.stompLocal = stompLocal;
        this.stompRemote = stompRemote;
        this.flee = flee;
        form.setItems(conflicts);
        init();
    }

    protected RunLater createAction(final RunLater runLater) {
        return new RunLater() {

            @Override
            public void run(Object arg) {
                runLater.run(arg);
                close(CANCEL_EXIT_CODE);
            }
        };
    }

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
