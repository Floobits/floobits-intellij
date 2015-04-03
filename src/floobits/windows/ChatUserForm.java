package floobits.windows;

import com.intellij.ui.components.JBList;
import com.intellij.uiDesigner.core.GridConstraints;
import floobits.common.RunLater;
import floobits.common.protocol.FlooUser;
import floobits.common.protocol.handlers.FlooHandler;
import floobits.dialogs.SetPermissionsDialog;
import floobits.impl.ContextImpl;
import floobits.utilities.Colors;
import floobits.utilities.Flog;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;

public class ChatUserForm {


    class ClickPanel extends JPanel {

        public ClickPanel(final Runnable clickHandler) {
            super();
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (clickHandler == null) {
                        return;
                    }
                    if (SwingUtilities.isRightMouseButton(e)) {
                        return;
                    }
                    clickHandler.run();
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    Cursor cursor = Cursor.getDefaultCursor();
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
                    setCursor(cursor);
                }
            });
        }
    }

    private final String gravatar;
    private JList clientList;
    private JPanel gravatarContainer;
    private JPanel containerPanel;
    private JPanel subContainer;
    private DefaultListModel clientModel;
    private JPopupMenu menuPopup;
    private ContextImpl context;
    private String username;
    private boolean following = false;
    private HashMap<Integer, ClientState> clients = new HashMap<Integer, ClientState>();

    protected class ClientChatActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            FlooHandler flooHandler = context.getFlooHandler();
            if (flooHandler == null) {
                return;
            }
            clientActionPerformed(flooHandler);
        }

        protected void clientActionPerformed(FlooHandler flooHandler) {}
    }

    static private class ClientState {
        public JMenuItem mi;
        public String label;
        public int userId;

        public ClientState(int userId, JMenuItem mi, String label) {
            this.userId = userId;
            this.mi = mi;
            this.label = label;
        }
    }

    private static class ClientCellRenderer extends JLabel implements ListCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            setText((String) value);
            setOpaque(false);
            return this;
        }
    }

    public ChatUserForm(ContextImpl context, String username, String gravatar) {
        this.context = context;
        this.username = username;
        this.gravatar = gravatar;
        setupPopupMenu();
        containerPanel.setComponentPopupMenu(menuPopup);
        updateBorder();
        updateGravatar();
    }

    public void setFollowing(boolean following) {
        this.following = following;
        updateBorder();
    }

    private void setFollowState(FlooHandler flooHandler, String msg) {
        flooHandler.state.setFollowedUsers(flooHandler.state.followedUsers);
        flooHandler.context.statusMessage(msg);
    }

    private void kickClient(int userId) {
        Flog.info("Kicking %s with user id %d.", username, userId);
        FlooHandler flooHandler = context.getFlooHandler();
        if (flooHandler == null) {
            return;
        }
        flooHandler.editorEventHandler.kick(userId);
    }

    private JMenuItem addKickMenuItem(final int userId, String label) {
        final JMenuItem kickMenuItem = new JMenuItem(label);
        kickMenuItem.addActionListener(new ClientChatActionListener() {
            @Override
            public void clientActionPerformed(FlooHandler flooHandler) {
                kickClient(userId);
            }
        });
        menuPopup.add(kickMenuItem);
        return kickMenuItem;
    }

    private void setupPopupMenu() {
        final JPopupMenu popupMenu = menuPopup;
        String kickAllLabel;
        if (username == null) {
            kickAllLabel = "Kick all clients for this user.";
        } else {
            kickAllLabel = String.format("Kick all of %s's clients", username);
        }
        final JMenuItem kickMenuItem = new JMenuItem(kickAllLabel);
        kickMenuItem.addActionListener(new ClientChatActionListener() {
            @Override
            public void clientActionPerformed(FlooHandler flooHandler) {
                for (ClientState client: clients.values()) {
                    kickClient(client.userId);
                }
            }
        });
        popupMenu.add(kickMenuItem);
        final JMenuItem followMenuItem = new JMenuItem("Follow");
        followMenuItem.addActionListener(new ClientChatActionListener() {
            @Override
            public void clientActionPerformed(FlooHandler flooHandler) {
                Flog.info("Following %s.", username);
                if (flooHandler.state.followedUsers.contains(username)) {
                    flooHandler.context.errorMessage(String.format("You are already following %s", username));
                    return;
                }
                flooHandler.state.followedUsers.add(username);
                setFollowState(flooHandler, String.format("You are now following %s", username));
                flooHandler.editorEventHandler.goToLastHighlight(username);
            }
        });
        followMenuItem.setVisible(false);
        popupMenu.add(followMenuItem);
        final JMenuItem unFollowMenuItem = new JMenuItem("Stop following");
        unFollowMenuItem.addActionListener(new ClientChatActionListener() {
            @Override
            public void clientActionPerformed(FlooHandler flooHandler) {
                Flog.info("Will stop following %s", username);
                if (!flooHandler.state.followedUsers.contains(username)) {
                    flooHandler.context.errorMessage(String.format("You are not following %s", username));
                    return;
                }
                flooHandler.state.followedUsers.remove(username);
                setFollowState(flooHandler, String.format("You have stopped following %s", username));
            }
        });
        unFollowMenuItem.setVisible(false);
        popupMenu.add(unFollowMenuItem);
        final JMenuItem adminMenuItem = new JMenuItem("Edit Permissions...");
        adminMenuItem.addActionListener(new ClientChatActionListener() {
            @Override
            public void clientActionPerformed(FlooHandler flooHandler) {
                final int userId = clients.entrySet().iterator().next().getKey();
                Flog.info("Opening up permission dialog for %s", username);
                FlooUser user = flooHandler.state.getUser(userId);
                if (user == null) {
                    return;
                }
                java.util.List<String> permissions = java.util.Arrays.asList(user.perms);
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
                setPermissionsDialog.setUsername(username);
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
                if (floohandler.state.username.equals(username)) {
                    return;
                }
                followMenuItem.setVisible(!following);
                unFollowMenuItem.setVisible(following);

            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
    }

    private void createUIComponents() {
        gravatarContainer = new ClickPanel(new Runnable() {
            @Override
            public void run() {
                FlooHandler flooHandler = context.getFlooHandler();
                if (flooHandler == null) {
                    return;
                }
                flooHandler.editorEventHandler.goToLastHighlight(username);
            }
        });
        subContainer = new JPanel();
        clientModel = new DefaultListModel();
        clientList = new JBList();
        clientList.setOpaque(false);
        clientList.setModel(clientModel);
        clientList.setCellRenderer(new ClientCellRenderer());
        menuPopup = new JPopupMenu();
        subContainer.setComponentPopupMenu(menuPopup);
        clientList.setComponentPopupMenu(menuPopup);
    }

    public void updateBorder() {
        TitledBorder border = (TitledBorder) containerPanel.getBorder();
        String usernameStr = username;
        if (following) {
            usernameStr += "*";
        }
        border.setTitle(usernameStr);
        containerPanel.validate();
    }

    public void updateGravatar() {
        if (gravatar == null) {
            return;
        }
        if (gravatarContainer.getComponentCount() != 0) {
            return;
        }
        ContextImpl.BalloonState balloonState = context.gravatars.get(gravatar);
        if (balloonState == null) {
            return;
        }
        JLabel iconlabel = new JLabel(new ImageIcon(balloonState.largeGravatar));
        iconlabel.setBorder(BorderFactory.createLineBorder(Colors.getColorForUser(username), 2));
        gravatarContainer.add(iconlabel, new GridConstraints());
        containerPanel.validate();

    }

    public void addClient(String client, String platform, int userId) {
        String label = String.format("<html>&middot; %s  <small><i>(%s)</html></i></small>", client, platform);
        clientModel.addElement(label);
        JMenuItem mi = addKickMenuItem(userId, String.format("<html>Kick %s <small><i>(%s)</html></i></small>", client, platform));
        clients.put(userId, new ClientState(userId, mi, label));
        containerPanel.validate();
    }

    private void refreshClientList() {
        clientModel.clear();
        for (ClientState client : clients.values()) {
            clientModel.addElement(client.label);
        }
        containerPanel.validate();
    }

    public void removeClient(int userId){
        ClientState client = clients.get(userId);
        if (client == null) {
            return;
        }
        clients.remove(userId);
        menuPopup.remove(client.mi);
        refreshClientList();
    }

    public int getNumClients() {
        return clients.size();
    }

    public JPanel getContainerPanel() {
        return containerPanel;
    }

}
