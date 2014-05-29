package floobits.windows;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import floobits.FlooContext;
import floobits.common.FlooUrl;
import floobits.common.handlers.FlooHandler;
import floobits.common.protocol.FlooUser;

import java.util.*;


public class ChatManager {
    protected FlooContext context;
    protected ToolWindow toolWindow;
    protected ChatForm chatForm;

    public ChatManager (FlooContext context) {
       this.context = context;
       chatForm = new ChatForm(context);
       this.createChatWindow(context.project);
    }

    protected void createChatWindow(Project project) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        toolWindow = toolWindowManager.registerToolWindow("Floobits Chat", true, ToolWindowAnchor.BOTTOM);
        toolWindow.setIcon(IconLoader.getIcon("/icons/floo.png"));
        Content content = ContentFactory.SERVICE.getInstance().createContent(chatForm.getChatPanel(), "", true);
        toolWindow.getContentManager().addContent(content);
    }

    public void openChat() {
        toolWindow.show(null);
        FlooHandler flooHandler = context.getFlooHandler();
        if (flooHandler == null) {
            return;
        }
        FlooUrl url = flooHandler.getUrl();
        toolWindow.setTitle(String.format("- %s", url.toString()));
    }

    public void closeChat() {
        toolWindow.hide(null);
    }

    public boolean isOpen() {
        try {
            return toolWindow.isVisible();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public void clearUsers() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                chatForm.clearClients();
            }
        }, ModalityState.NON_MODAL);
    }

    public void setUsers(final Map<Integer,FlooUser> originalUsers) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                // Copy users so we don't modify the flooHandler user list:
                HashMap<Integer, FlooUser> users = new HashMap<Integer, FlooUser>();
                users.putAll(originalUsers);

                // Clear existing stuff:
                chatForm.clearClients();

                // Created a sorted list by username.
                Iterator usersIterator = users.entrySet().iterator();
                ArrayList<FlooUser> userList = new ArrayList<FlooUser>();
                while (usersIterator.hasNext()) {
                    Map.Entry user = (Map.Entry) usersIterator.next();
                    userList.add((FlooUser) user.getValue());
                    usersIterator.remove();
                }
                Collections.sort(userList, new Comparator<FlooUser>() {
                   @Override
                   public int compare(FlooUser a, FlooUser b) {
                        return a.username.compareTo(b.username);
                    }
                });

                // Add the list to the model.
                for (FlooUser user : userList) {
                    chatForm.addClient(user.username, user.client, user.platform, user.user_id);
                }
            }
        }, ModalityState.NON_MODAL);
    }

    public void statusMessage(final String message) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                chatForm.statusMessage(message);
            }
        }, ModalityState.NON_MODAL);
    }

    public void errorMessage(final String message) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                chatForm.errorMessage(message);
            }
        }, ModalityState.NON_MODAL);
    }

    public void chatMessage(final String username, final String msg, final Date messageDate) {
        if (context.lastChatMessage != null && context.lastChatMessage.compareTo(messageDate) > -1) {
            // Don't replay previously shown chat messages.
            return;
        }
        context.lastChatMessage = messageDate;
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                chatForm.chatMessage(username, msg, messageDate);
            }
        }, ModalityState.NON_MODAL);
    }
}




