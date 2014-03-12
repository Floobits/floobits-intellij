package floobits.windows;

import javax.swing.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatForm {
    private JPanel chatPanel;
    private JList messages;
    private DefaultListModel messagesModel = new DefaultListModel();
    private DefaultListModel clientModel = new DefaultListModel();
    private JList clients;
    private JScrollPane messagesScrollPane;
    private boolean shouldScrollToBottom;

    public ChatForm () {
        super();
        messages.setModel(messagesModel);
        clients.setModel(clientModel);
        messagesScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (shouldScrollToBottom) {
                    e.getAdjustable().setValue(e.getAdjustable().getMaximum());
                }
                shouldScrollToBottom = false;
            }
        });
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

    public void statusMessage(String message) {
        String messageFormat = "<html>%s<b><i><span style=\"color:green\">%s</span></i></b></html>";
        addMessage(String.format(messageFormat, stampMessage("*status*", null), message));
    }

    public void errorMessage(String message) {
        String messageFormat = "<html>%s<b><i><span style=\"color:red\">%s</span></i></b></html>";
        addMessage(String.format(messageFormat, stampMessage("*error*", null), message));
    }

    public void chatMessage(String username, String msg, Date messageDate) {
        addMessage(String.format("<html>%s%s</html>", stampMessage(username, messageDate), msg));
    }

    protected void addMessage(String msg) {
        shouldScrollToBottom = true;
        messagesModel.addElement(msg);
    }

    protected String stampMessage(String nick, Date when) {
        if (when == null) {
            when = new Date();
        }
        return String.format("<span style=\"color:gray;\">[%s]</span> <b>%s</b>: ", new SimpleDateFormat("HH:mm:ss").format(when), nick);
    }
}
