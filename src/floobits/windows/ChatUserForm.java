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

import javax.naming.Context;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class ChatUserForm {
    private JList clientList;
    private JPanel gravatarContainer;
    private JPanel containerPanel;
    private JPanel subContainer;
    private DefaultListModel clientModel;
    private JMenuItem testMenuItem;
    private JPopupMenu menuPopup;
    private ContextImpl context;


    protected class ClientChatActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            FlooHandler flooHandler = context.getFlooHandler();
            if (flooHandler == null) {
                return;
            }
        }

        protected void clientActionPerformed(FlooHandler flooHandler, ClientModelItem client) {

        }
    }


    static public class ClientModelItem {
        public String username;
        public String client;
        public String platform;
        public String gravatar;
        public int userId;
        public Boolean following;

        public ClientModelItem(String username, String gravatar, String client, String platform, Integer userId, Boolean following) {
            this.username = username;
            this.client = client;
            this.platform = platform;
            this.userId = userId;
            this.following = following;
            this.gravatar = gravatar;
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

    public ChatUserForm (ContextImpl context) {
        this.context = context;
        setupPopupMenu();
        containerPanel.setComponentPopupMenu(menuPopup);
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
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
    }

    private void createUIComponents() {
        subContainer = new JPanel();
        clientModel = new DefaultListModel();
        clientList = new JBList();
        clientList.setOpaque(false);
        clientList.setModel(clientModel);
        clientList.setCellRenderer(new ClientCellRenderer());
        testMenuItem = new JMenuItem("test");
        menuPopup = new JPopupMenu();
        menuPopup.add(testMenuItem);
        subContainer.setComponentPopupMenu(menuPopup);
        clientList.setComponentPopupMenu(menuPopup);
    }

    public void setUsername(String username) {
        TitledBorder border = (TitledBorder) containerPanel.getBorder();
        border.setTitle(username);
    }

    public void addGravatar(Image gravatar, String username) {
        JLabel iconlabel = new JLabel(new ImageIcon(gravatar));
        iconlabel.setBorder(BorderFactory.createLineBorder(Colors.getColorForUser(username), 2));
        gravatarContainer.add(iconlabel, new GridConstraints());

    }

    public void addClient(String client, String platform) {
        clientModel.addElement(String.format("<html>&middot; %s  <small><i>(%s)</html></i></small>", client, platform));
    }

    public JPanel getContainerPanel() {
        return containerPanel;
    }

}
