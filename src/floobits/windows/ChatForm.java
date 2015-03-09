package floobits.windows;

import com.intellij.ui.JBColor;
import floobits.common.interfaces.IContext;
import floobits.common.RunLater;
import floobits.common.protocol.handlers.FlooHandler;
import floobits.common.protocol.FlooUser;
import floobits.dialogs.SetPermissionsDialog;
import floobits.utilities.Colors;
import floobits.utilities.Flog;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ChatForm {

    protected class ClientChatActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            FlooHandler flooHandler = context.getFlooHandler();
            if (flooHandler == null) {
                return;
            }
        }

        protected void clientActionPerformed(FlooHandler flooHandler, ClientModelItem client) {}
    }

    protected class ClientModelItem {
        public String username;
        public String client;
        public String platform;
        public int userId;
        public Boolean following;

        public ClientModelItem(String username, String client, String platform, Integer userId, Boolean following) {
            this.username = username;
            this.client = client;
            this.platform = platform;
            this.userId = userId;
            this.following = following;
        }

        public String toString() {
            String formattedUsername = username;
            if (following) {
                formattedUsername += "*";
            }
            return String.format("<html><b>%s</b> <small><i>%s (%s)</html></i></small>", formattedUsername, client, platform);
        }
    }

    private JPanel chatPanel;
    private JScrollPane messagesScrollPane;
    private JButton chatButton;
    private JTextField chatInput;
    private JTextPane messages;
    private HTMLEditorKit kit;
    private HTMLDocument doc;
    private IContext context;
    private boolean shouldScrollToBottom;

    public ChatForm (IContext context) {
        super();
        this.context = context;
        setupPopupMenu();
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

    private void setFollowState(FlooHandler flooHandler, String msg) {
        flooHandler.state.setFollowedUsers(flooHandler.state.followedUsers);
        flooHandler.context.setUsers(flooHandler.state.users);
        flooHandler.context.statusMessage(msg);
    }

    private void setupPopupMenu() {
        final JPopupMenu popupMenu = new JPopupMenu();
        final JMenuItem kickMenuItem = new JMenuItem("Kick");
        kickMenuItem.addActionListener(new ClientChatActionListener() {
            @Override
            public void clientActionPerformed(FlooHandler flooHandler, ClientModelItem client) {
                Flog.info("Kicking %s with user id %d.", client.username, client.userId);
                flooHandler.editorEventHandler.kick(client.userId);
            }
        });
        popupMenu.add(kickMenuItem);
        final JMenuItem followMenuItem = new JMenuItem("Follow");
        followMenuItem.addActionListener(new ClientChatActionListener() {
            @Override
            public void clientActionPerformed(FlooHandler flooHandler, ClientModelItem client) {
                Flog.info("Following %s with user id %d.", client.username, client.userId);
                if (flooHandler.state.followedUsers.contains(client.username)) {
                    flooHandler.context.errorMessage(String.format("You are already following %s", client.username));
                    return;
                }
                flooHandler.state.followedUsers.add(client.username);
                setFollowState(flooHandler, String.format("You are now following %s", client.username));
            }
        });
        popupMenu.add(followMenuItem);
        final JMenuItem unFollowMenuItem = new JMenuItem("Stop following");
        unFollowMenuItem.addActionListener(new ClientChatActionListener() {
            @Override
            public void clientActionPerformed(FlooHandler flooHandler, ClientModelItem client) {
                Flog.info("Will stop following %s with user id %d.", client.username, client.userId);
                if (!flooHandler.state.followedUsers.contains(client.username)) {
                    flooHandler.context.errorMessage(String.format("You are not following %s", client.username));
                    return;
                }
                flooHandler.state.followedUsers.remove(client.username);
                setFollowState(flooHandler, String.format("You have stopped following %s", client.username));
            }
        });
        popupMenu.add(unFollowMenuItem);
        final JMenuItem adminMenuItem = new JMenuItem("Edit Permissions...");
        adminMenuItem.addActionListener(new ClientChatActionListener() {
            @Override
            public void clientActionPerformed(FlooHandler flooHandler, ClientModelItem client) {
                final int userId = client.userId;
                Flog.info("Opening up permission dialog for %s", client.username);
                FlooUser user = flooHandler.state.getUser(client.userId);
                if (user == null) {
                    return;
                }
                List<String> permissions = java.util.Arrays.asList(user.perms);
                SetPermissionsDialog setPermissionsDialog = new SetPermissionsDialog(
                        new RunLater<String[]>() {
                            @Override
                            public void run(String[] permissions) {
                                Flog.info("Submitting permission changes.");
                                FlooHandler flooHandler = context.getFlooHandler();
                                if (flooHandler == null) {
                                    return;
                                }
                                flooHandler.editorEventHandler.changePerms(userId, permissions);
                            }
                        },
                        permissions.contains("get_buf"),
                        permissions.contains("request_perms"),
                        permissions.contains("patch"),
                        permissions.contains("kick")
                );
                setPermissionsDialog.setUsername(client.username);
                setPermissionsDialog.createCenterPanel();
                setPermissionsDialog.show();
            }
        });
        popupMenu.add(adminMenuItem);
        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                FlooHandler floohandler = context.getFlooHandler();
                if (floohandler == null) {
                    return;
                }
                kickMenuItem.setEnabled(floohandler.state.can("kick"));
                adminMenuItem.setEnabled(floohandler.state.can("kick"));

            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {}
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
        flooHandler.editorEventHandler.msg(chatContents);
        chatInput.setText("");
        chatMessage(flooHandler.state.getUsername(flooHandler.state.getMyConnectionId()), chatContents, null);
    }

    public JPanel getChatPanel() {
        return chatPanel;
    }

    public void clearClients() {
    }

    public void addClient(String username, String client, String platform, Integer user_id, Boolean following) {
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
