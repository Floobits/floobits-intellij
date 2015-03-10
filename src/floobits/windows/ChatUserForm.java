package floobits.windows;

import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import java.awt.*;

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

    public void addGravatar(Image gravatar) {
        JLabel iconlabel = new JLabel(new ImageIcon(gravatar));
        gravatarContainer.add(iconlabel, new GridConstraints());

    }

    public JPanel getContainerPanel() {
        return containerPanel;
    }

}
