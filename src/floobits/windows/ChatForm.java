package floobits.windows;

import com.intellij.ui.JBColor;
import floobits.FlooContext;
import floobits.common.handlers.FlooHandler;
import floobits.utilities.Colors;
import floobits.utilities.Flog;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatForm {

    protected class ClientModelItem {
        public String username;
        public String client;
        public String platform;
        public int userId;

        public ClientModelItem(String username, String client, String platform, Integer userId) {
            this.username = username;
            this.client = client;
            this.platform = platform;
            this.userId = userId;
        }

        public String toString() {
            return String.format("<html><b>%s</b> <small><i>%s (%s)</html></i></small>", username, client, platform);
        }
    }

    private JPanel chatPanel;
    private DefaultListModel clientModel = new DefaultListModel();
    private JList clients;
    private JScrollPane messagesScrollPane;
    private JButton chatButton;
    private JTextField chatInput;
    private JTextPane messages;
    private HTMLEditorKit kit;
    private HTMLDocument doc;
    private FlooContext context;
    private boolean shouldScrollToBottom;

    public ChatForm (FlooContext context) {
        super();
        this.context = context;
        clients.setModel(clientModel);
        setupPopupMenu();
        kit = new HTMLEditorKit();
        doc = new HTMLDocument();
        messages.setEditorKit(kit);
        messages.setDocument(doc);
        messages.setEditable(false);
        try {
            kit.insertHTML(doc, doc.getLength(), "<style>body { color: #7f7f7f;}</style>", 0, 0, null);
        } catch (Exception e) {
            Flog.warn("Couldn't insert initial HTML into chat %s.", e.toString());
        }
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

    private void setupPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem kickMenuItem = new JMenuItem("Kick");
        kickMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ClientModelItem item = (ClientModelItem) clients.getSelectedValue();
                Flog.info("Kicking %s with user id %d.", item.username, item.userId);
                context.getFlooHandler().untellij_kick(item.userId);
            }
        });
        popupMenu.add(kickMenuItem);
        clients.setComponentPopupMenu(popupMenu);
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

    public void addClients(String username, String client, String platform, Integer user_id) {
        clientModel.addElement(new ClientModelItem(username, client, platform, user_id));
    }

    public void statusMessage(String message) {
        String messageFormat = "%s<b><i><span style=\"color:green\">%s</span></i></b><br/>";
        addMessage(String.format(messageFormat, stampMessage("*status*", null), message));
    }

    public void errorMessage(String message) {
        String messageFormat = "%s<b><i><span style=\"color:red\">%s</span></i></b><br/>";
        addMessage(String.format(messageFormat, stampMessage("*error*", null), message));
    }

    public void chatMessage(String username, String msg, Date messageDate) {
        FlooHandler flooHandler = context.getFlooHandler();
        if (flooHandler == null) {
            return;
        }
        addMessage(String.format("%s%s<br/>", stampMessage(username, messageDate), msg));
    }

    protected void addMessage(String msg) {
        shouldScrollToBottom = true;
        try {
            kit.insertHTML(doc, doc.getLength(), msg, 0, 0, null);
        } catch (IOException e) {
            Flog.warn("Was unable to add message to chat: %s at %d because %s", msg, doc.getLength(), e.toString());
        } catch (BadLocationException e) {
            Flog.warn("Was unable to add message to chat: %s at %d because %s", msg, doc.getLength(), e.toString());
        }
    }

    protected String stampMessage(String nick, Date when) {
        if (when == null) {
            when = new Date();
        }
        String displayNick = nick;
        if (!nick.equals("*error*") && !nick.equals("*status*")) {
            JBColor color = Colors.getColorForUser(nick);
            String rgba = Colors.getHex(color);
            displayNick = String.format(" <span style=\"background-color:%s;color:white;\">&nbsp;</span> %s ", rgba, nick);
        }
        return String.format("<span style=\"color:gray;\">[%s]</span> <b>%s</b>: ", new SimpleDateFormat("HH:mm:ss").format(when), displayNick);
    }
}
