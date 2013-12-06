package floobits;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.DialogWrapperPeer;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class OkCancelDialog extends DialogWrapper {
    private JPanel jPanel;

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return jPanel;
    }

    protected OkCancelDialog(String title, String pregunta) {
        super(true);
        jPanel = new JPanel();
        init();
        DialogWrapperPeer peer = this.getPeer();
        peer.setTitle(title);

        JLabel body = new JLabel();
        body.setText(String.format("<html><h2>%s</h2></html>", pregunta));
        jPanel.add(body);
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
    }
}