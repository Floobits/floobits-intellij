package floobits.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import floobits.common.RunLater;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CustomButtonDialogWrapper extends DialogWrapper {
    protected Action[] actions;

    protected CustomButtonDialogWrapper(boolean canBeParent) {
        super(canBeParent);
    }

    protected CustomButtonDialogWrapper(Project project, boolean canBeParent) {
        super(project, canBeParent);
    }

    protected class CustomButtonAction extends DialogWrapperAction {
        protected RunLater<Void> action;

        protected CustomButtonAction(String name, RunLater<Void> action) {
            super(name);
            this.action = action;
        }

        @Override
        protected void doAction(ActionEvent e) {
            if (action != null) {
                action.run(null);
            }
            close(OK_EXIT_CODE);
        }

    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return null;
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return actions;
    }
}
