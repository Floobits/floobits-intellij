package floobits.windows;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import floobits.FlooContext;


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
        Content content = ContentFactory.SERVICE.getInstance().createContent(chatForm.getChatPanel(), "Floobits Chat", true);
        toolWindow.getContentManager().addContent(content);
    }

    public void openChat() {
        toolWindow.show(null);
    }

    public void closeChat() {
        toolWindow.hide(null);
    }
}




