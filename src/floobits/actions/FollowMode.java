package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.handlers.FlooHandler;

public class FollowMode extends IsJoinedAction {
    public void actionPerformed(AnActionEvent e, FlooHandler flooHandler) {
        boolean mode = !flooHandler.stalking;
        flooHandler.context.statusMessage(String.format("%s follow mode", mode ? "Enabling" : "Disabling"));
        e.getPresentation().setText(String.format("%s follow mode", mode ? "Disable" : "Enable"));
        flooHandler.stalking = mode;
        if (mode && flooHandler.lastHighlight != null) {
            flooHandler._on_highlight(flooHandler.lastHighlight);
        }
    }
}
