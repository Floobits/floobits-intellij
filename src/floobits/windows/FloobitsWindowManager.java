package floobits.windows;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import floobits.common.FlooUrl;
import floobits.common.interfaces.IContext;
import floobits.common.protocol.FlooUser;
import floobits.common.protocol.handlers.FlooHandler;
import floobits.impl.ContextImpl;
import org.jetbrains.annotations.NotNull;


import java.util.Date;


public class FloobitsWindowManager {
    protected IContext context;
    protected ToolWindow toolWindow;
    protected FloobitsWindowForm chatForm;

    public FloobitsWindowManager(ContextImpl context) {
       this.context = context;
       chatForm = new FloobitsWindowForm(context);
       this.createChatWindow(context.project);
    }

    protected void createChatWindow(Project project) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        toolWindow = toolWindowManager.registerToolWindow("Floobits", true, ToolWindowAnchor.BOTTOM);
        toolWindow.setIcon(IconLoader.getIcon("/icons/floo13.png"));
        Content content = ContentFactory.SERVICE.getInstance().createContent(chatForm.getChatPanel(), "", true);
        toolWindow.getContentManager().addContent(content);
        updateTitle();
    }

    public void openFloobitsWindow() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                if (toolWindow == null) {
                    return;
                }
                toolWindow.show(null);
            }
        });
    }

    public void closeFloobitsWindow() {
        toolWindow.hide(null);
    }

    public void toggleFloobitsWindow() {
        if (isOpen()) {
            closeFloobitsWindow();
        } else {
            openFloobitsWindow();
        }
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
                updateTitle();
            }
        }, ModalityState.NON_MODAL);
    }

    public void addUser(final FlooUser user) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                chatForm.addUser(user);
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

    public void removeUser(final FlooUser user) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                chatForm.removeUser(user);
            }
        }, ModalityState.NON_MODAL);
    }

    public void updateUserList() {
        Application app = ApplicationManager.getApplication();
        if (app == null) {
            return;
        }
        app.invokeLater(new Runnable() {
            @Override
            public void run() {
                FlooHandler flooHandler = context.getFlooHandler();
                if (flooHandler == null) {
                    return;
                }
                chatForm.updateGravatars();
                chatForm.updateFollowing(flooHandler.state.followedUsers);
                updateTitle();
            }
        }, ModalityState.NON_MODAL);
    }

    protected void updateTitle() {
        FlooHandler flooHandler = context.getFlooHandler();
        if (flooHandler == null) {
            toolWindow.setTitle("- not currently connected.");
            return;
        }
        FlooUrl url = flooHandler.getUrl();
        int numClients = flooHandler.state.users.size();
        toolWindow.setTitle(String.format(
                "- %s as %s. %d client%s connected.",
                url.toString(),
                flooHandler.state.username,
                numClients,
                numClients == 1 ? "" : "s"
        ));
    }
}
