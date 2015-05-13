package floobits.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
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
        protected Runnable action;
        protected int exitCode = OK_EXIT_CODE;

        protected CustomButtonAction(String name, Runnable action) {
            super(name);
            this.action = action;
        }

        protected CustomButtonAction(String name, Runnable action, int exitCode) {
            super(name);
            this.action = action;
            this.exitCode = exitCode;
        }

        @Override
        protected void doAction(ActionEvent e) {
            if (action != null) {
                action.run();
            }
            close(exitCode);
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
