package floobits.windows;

import javax.swing.*;

public class ChatForm {
    private JPanel chatPanel;
    private JList messages;
    private DefaultListModel messagesModel = new DefaultListModel();
    private DefaultListModel clientModel = new DefaultListModel();
    private JList clients;
    private JTextField textField1;

    public ChatForm () {
        super();
        messages.setModel(messagesModel);
        clients.setModel(clientModel);
    }

    public JPanel getChatPanel() {
        return chatPanel;
    }

    public void clearClients() {
        clientModel.clear();
    }

    public void addClients(String username, String client, String platform) {
        clientModel.addElement(String.format("<html><b>%s</b> <small><i>%s (%s)</html></i></small>", username, client, platform));
    }
}
