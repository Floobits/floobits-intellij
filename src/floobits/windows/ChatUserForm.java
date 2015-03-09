package floobits.windows;

import javax.swing.*;

/**
 * Created by bjorn on 3/6/15.
 */
public class ChatUserForm {
    private JList clientList;
    private JLabel usernameLabel;
    private JPanel gravatarContainer;
    private JPanel containerPanel;

    public void setUsername(String username) {
        usernameLabel.setText(username);
    }

    public JPanel getContainerPanel() {
        return containerPanel;
    }
}
