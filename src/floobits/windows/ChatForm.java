package floobits.windows;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import floobits.common.interfaces.IContext;
import floobits.common.protocol.handlers.FlooHandler;
import floobits.impl.ContextImpl;
import floobits.utilities.Colors;
import floobits.utilities.Flog;
import floobits.windows.ChatUserForm.ClientModelItem;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Date;

public class ChatForm {


    private JPanel chatPanel;
    private JScrollPane messagesScrollPane;
    private JButton chatButton;
    private JTextField chatInput;
    private JTextPane messages;
    private JScrollPane clientsScrollPane;
    private JPanel clientsPane;
    private HTMLEditorKit kit;
    private HTMLDocument doc;
    private ContextImpl context;
    private boolean shouldScrollToBottom;
    private HashMap<String, ChatUserForm> userForms = new HashMap<String, ChatUserForm>();

    public ChatForm (IContext context) {
        super();
        this.context = (ContextImpl) context;
        kit = new HTMLEditorKit();
        doc = new HTMLDocument();
        messages.setEditorKit(kit);
        messages.setDocument(doc);
        messages.setEditable(false);
        messages.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        URI uri = e.getURL().toURI();
                        Desktop.getDesktop().browse(uri);
                    } catch (IOException error) {
                        Flog.warn(error);
                    } catch (URISyntaxException error) {
                        Flog.warn(error);
                    }
                }
            }
        });
        try {
            kit.insertHTML(doc, doc.getLength(), "<style>body { color: #7f7f7f;}</style>", 0, 0, null);
        } catch (Throwable e) {
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
               Flog.log("Got action from chat button");
               sendChatContents();
            }
        });
    }


    private void createUIComponents() {
        clientsScrollPane = new JBScrollPane();
        clientsPane = new JPanel();
        clientsScrollPane.setViewportView(clientsPane);
        clientsPane.setLayout(new BoxLayout(clientsPane, BoxLayout.Y_AXIS));
    }

    public void clearClients() {
        clientsPane.removeAll();
    }

    public void addUser(List<ClientModelItem> clients) {
        ChatUserForm.ClientModelItem firstClient = clients.get(0);
        if (firstClient == null) {
            return;
        }
        ChatUserForm userForm = new ChatUserForm(context, firstClient.username);
        userForm.setUsername(firstClient.username);
        if (firstClient.gravatar != null) {
            ContextImpl.BalloonState balloonState = context.gravatars.get(firstClient.gravatar);
            if (balloonState != null) {
                userForm.addGravatar(balloonState.largeGravatar, firstClient.username);
            }
        }
        for (ChatUserForm.ClientModelItem client : clients) {
            userForm.addClient(client.client, client.platform, client.userId);
        }
        clientsPane.add(userForm.getContainerPanel());
        userForms.put(firstClient.username, userForm);
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
        flooHandler.editorEventHandler.msg(chatContents);
        chatInput.setText("");
        chatMessage(flooHandler.state.getUsername(flooHandler.state.getMyConnectionId()), chatContents, null);
    }

    public JPanel getChatPanel() {
        return chatPanel;
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

    public void removeUser(Integer userId, String username) {
        ChatUserForm userForm = userForms.get(username);
        if (userForm == null) {
            Flog.info("Could not find userForm for %d %s when trying to remove a user.", userId, username);
            return;
        }
        userForm.removeClient(userId);
        if (userForm.getNumClients() < 1) {
            clientsPane.remove(userForm.getContainerPanel());
            userForms.remove(username);
        }
    }
}
