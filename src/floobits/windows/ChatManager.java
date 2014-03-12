package floobits.windows;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import floobits.FlooContext;
import floobits.common.protocol.FlooUser;

import java.util.*;


public class ChatManager {
    protected FlooContext context;
    protected ToolWindow toolWindow;
    protected ChatForm chatForm = new ChatForm();

    public ChatManager (FlooContext context) {
       this.context = context;
       this.createChatWindow(context.project);
    }

    protected void createChatWindow(Project project) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        toolWindow = toolWindowManager.registerToolWindow("Floobits Chat", true, ToolWindowAnchor.BOTTOM);
        Content content = ContentFactory.SERVICE.getInstance().createContent(chatForm.getChatPanel(), "", true);
        toolWindow.getContentManager().addContent(content);
    }

    public void openChat() {
        toolWindow.show(null);
    }

    public void closeChat() {
        toolWindow.hide(null);
    }

    public boolean isOpen() {
        return toolWindow.isVisible();
    }

    public void clearUsers() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                chatForm.clearClients();
            }
        });
    }

    public void setUsers(final Map<Integer,FlooUser> originalUsers) {

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                // Copy users so we don't modify the floohandler user list:
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
                    chatForm.addClients(user.username, user.client, user.platform);
                }
            }
        });
    }

    public void statusMessage(final String message) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                chatForm.statusMessage(message);
            }
        });
    }

    public void errorMessage(final String message) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                chatForm.errorMessage(message);
            }
        });
    }

    public void chatMessage(final String username, final String msg, final Date messageDate) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                chatForm.chatMessage(username, msg, messageDate);
            }
        });
    }
}




