package floobits.dialogs;

import floobits.common.RunLater;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class DisconnectNoticeDialog extends CustomButtonDialogWrapper {
    protected JPanel container;
    protected JLabel reasonLabel;

    public DisconnectNoticeDialog(final RunLater<Void> runLater, final String reason) {
        super(true);
        container = new JPanel();
        reasonLabel = new JLabel();
        reasonLabel.setText(reason);
        container.add(reasonLabel);
        CustomButtonAction disconnectAction = new CustomButtonAction("Disconnect", new RunLater<Void>() {
            @Override
            public void run(Void arg) {
                runLater.run(null);
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
