package floobits.windows;

import com.intellij.ui.JBColor;
import floobits.FlooContext;
import floobits.common.Utils;
import floobits.handlers.FlooHandler;
import floobits.utilities.Colors;
import floobits.utilities.Flog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    private JButton chatButton;
    private JTextField chatInput;
    private FlooContext context;
    private boolean shouldScrollToBottom;

    public ChatForm (FlooContext context) {
        super();
        this.context = context;
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
        chatInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Flog.log("Got action in chat");
                sendChatContents();
            }
        });
        chatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
               Flog.log("Gog action from chat button");
               sendChatContents();
            }
        });
    }

    private void sendChatContents() {
        FlooHandler flooHandler = context.getFlooHandler();
        if (flooHandler == null) {
            return;
        }
        String chatContents = chatInput.getText().trim();
        if (chatContents.length() < 1) {
            return;
        }
        flooHandler.untellij_msg(chatContents);
        chatInput.setText("");
        chatMessage(flooHandler.getUsername(flooHandler.getMyConnectionId()), chatContents, null);
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
        FlooHandler flooHandler = context.getFlooHandler();
        if (flooHandler == null) {
            return;
        }
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
        String displayNick = nick;
        if (!nick.equals("*error*") && !nick.equals("*status*")) {
            JBColor color = Colors.getColorForUser(nick);
            String rgba = Colors.getHex(color);
            displayNick = String.format("<span style=\"background-color:%s;color:white;\">%s</span", rgba, nick);
        }
        return String.format("<span style=\"color:gray;\">[%s]</span> <b>%s</b>: ", new SimpleDateFormat("HH:mm:ss").format(when), displayNick);
    }
}
