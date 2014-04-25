package floobits.dialogs;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class DisconnectNoticeDialog extends CustomButtonDialogWrapper {
    protected JPanel container;
    protected JLabel reasonLabel;

    public DisconnectNoticeDialog(final Runnable runLater, final String reason) {
        super(true);
        container = new JPanel();
        reasonLabel = new JLabel();
        reasonLabel.setText(reason);
        container.add(reasonLabel);
        CustomButtonAction disconnectAction = new CustomButtonAction("Disconnect", new Runnable() {
            @Override
            public void run() {
                runLater.run();
            }
        });
        actions = new Action[]{disconnectAction};
        init();
    }

    @Nullable
    @Override
    public JComponent createCenterPanel() {
        return container;

    }
}
