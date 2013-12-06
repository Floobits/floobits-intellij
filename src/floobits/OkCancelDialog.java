package floobits;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.DialogWrapperPeer;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class OkCancelDialog extends DialogWrapper {
    private JPanel jPanel;
    private RunLater runLater;

    public static void build(final String title, final String body, final RunLater runLater) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                OkCancelDialog okCancelDialog = new OkCancelDialog(title, body, runLater);
                okCancelDialog.createCenterPanel();
                okCancelDialog.show();
            }
        });
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return jPanel;
    }

    protected OkCancelDialog(String title, String body, RunLater runLater) {
        super(true);
        jPanel = new JPanel();
        this.runLater = runLater;
        init();
        DialogWrapperPeer peer = this.getPeer();
        peer.setTitle(title);

        JLabel jLabel = new JLabel();
        jLabel.setText(String.format("<html>%s</html>", body));
        jPanel.add(jLabel);
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
        runLater.run(false);
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        runLater.run(true);
    }
}