package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.common.EditorEventHandler;

public class ChatAction extends IsJoinedAction {
    @Override
    public void actionPerformed(AnActionEvent e, EditorEventHandler editorEventHandler) {
        // chatManager will always be available, because you can't open chat unless you are logged in.
        editorEventHandler.openChat();
    }
}
