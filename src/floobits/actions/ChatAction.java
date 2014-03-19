package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.common.handlers.FlooHandler;
import floobits.utilities.Flog;

public class ChatAction extends IsJoinedAction {
    @Override
    public void actionPerformed(AnActionEvent e, FlooHandler flooHandler) {
        Flog.info("Showing user window.");
        // chatManager will always be available, because you can't open chat unless you are logged in.
        flooHandler.context.chatManager.openChat();
    }
}
