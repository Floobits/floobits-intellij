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

    protected OkCancelDialog(String title, boolean canBeParent) {
        super(canBeParent);
        jPanel = new JPanel();
        init();
        DialogWrapperPeer peer = this.getPeer();
        peer.setTitle(title);
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
    }
}