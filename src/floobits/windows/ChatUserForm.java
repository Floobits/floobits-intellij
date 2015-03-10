package floobits.windows;

import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import java.awt.*;

public class ChatUserForm {
    private JList clientList;
    private JLabel usernameLabel;
    private JPanel gravatarContainer;
    private JPanel containerPanel;
    private DefaultListModel clientModel = new DefaultListModel();

    public ChatUserForm() {
        clientList.setModel(clientModel);
    }

        public void setUsername(String username) {
        usernameLabel.setText(username);
    }

    public void addGravatar(Image gravatar) {
        JLabel iconlabel = new JLabel(new ImageIcon(gravatar));
        gravatarContainer.add(iconlabel, new GridConstraints());

    }

    public void addClient(String client, String platform) {
        clientModel.addElement(String.format("%s %s", client, platform));
    }

    public JPanel getContainerPanel() {
        return containerPanel;
    }

}
