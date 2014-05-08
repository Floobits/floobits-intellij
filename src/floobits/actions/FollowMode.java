package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.common.EditorEventHandler;

public class FollowMode extends IsJoinedAction {
    public void actionPerformed(AnActionEvent e, EditorEventHandler editorEventHandler) {
        boolean mode = editorEventHandler.follow();
        e.getPresentation().setText(String.format("%s follow mode", mode ? "Disable" : "Enable"));
    }
}
