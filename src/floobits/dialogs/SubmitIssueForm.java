package floobits.dialogs;

import floobits.common.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

public class SubmitIssueForm {
    private JPanel contentContainer;
    private JTextArea descriptionsTA;
    private JButton submitButton;
    private JButton cancelButton;
    private JTextField usernameInput;
    private JLabel instructionsLabel;
    private JFrame frame;

    public SubmitIssueForm() {
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.setVisible(false);
                frame.dispose();
            }
        });
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String description = descriptionsTA.getText();
                String username = usernameInput.getText();
                API.sendUserIssue(description, username);
                frame.setVisible(false);
                frame.dispose();
            }
        });
    }


    public void show() {
        FloorcJson floorcJson = null;
        try {
            floorcJson = Settings.get();
        } catch (Throwable ignored) {}

        HashMap<String, String> auth = floorcJson != null ? floorcJson.auth.get(Utils.getDefaultHost()) : null;
        String username = "?";
        if (auth != null) {
            username = auth.get("username");
        }
        usernameInput.setText(username);
        frame = new JFrame();
        frame.getContentPane().add(contentContainer);
        frame.setMinimumSize(new Dimension(650, 500));
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.pack();
        contentContainer.setBorder(new EmptyBorder(5, 5, 5, 5));
        frame.setVisible(true);
        String contents = "<html><body><p>When you submit an "
                + "issue we will be notified right away. If we have contact information for you<br/> we will respond. You can also "
                + "contact us via support@floobits.com, on IRC in #floobits<br/> "
                + "on Freenode, or via @floobits Twitter. If you run into "
                + "a bug it may help us if you send us your <br/>log. You can find it by going to Help -&gt; Find "
                + "log....     </p></body></html>";

        instructionsLabel.setText(contents);
        instructionsLabel.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
    }
}
