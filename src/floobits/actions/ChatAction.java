package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.handlers.FlooHandler;
import floobits.utilities.Flog;

public class ChatAction extends IsJoinedAction {
    @Override
    public void actionPerformed(AnActionEvent e, FlooHandler flooHandler) {
        Flog.info("Showing user window.");
    }
}
