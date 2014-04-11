package floobits.dialogs;

import com.intellij.openapi.ui.DialogWrapper;
import floobits.common.Ignore;
import floobits.common.RunLater;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.LinkedList;


public class HandleTooBigDialog extends DialogWrapper {
    private JPanel jPanel;
    private RunLater<Boolean> runnable;

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return jPanel;
    }

    public HandleTooBigDialog(final LinkedList<Ignore> tooBig, RunLater<Boolean> runnable) {
        super(true);
        this.runnable = runnable;
        jPanel = new JPanel();
        init();
        this.setTitle("This Project Is Too Large");

        JLabel jLabel = new JLabel();
        String text = "<html>Ignore the following directories and continue?";

        for (Ignore folder: tooBig) {
            text += String.format("<p>%s: %s</p>", folder.file.getPath(), folder.size);
        }
        text += "</html>";
        jLabel.setText(text);
        jPanel.add(jLabel);
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
        runnable.run(false);
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        runnable.run(true);
    }
}