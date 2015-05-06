package floobits.dialogs;

import javax.swing.*;
import java.awt.*;

public class MessageContainer {
    private JPanel content;
    private JLabel messageLabel;
    private JPanel container;

    public MessageContainer(JPanel content, String message) {
        messageLabel.setText(String.format("<html><span style=\"font-style: italic;\">%s</pan></html>", message));
        this.content.setLayout(new BorderLayout());
        this.content.add(content, BorderLayout.NORTH);
    }

    public JPanel getContentPanel() {
        return container;
    }
}
