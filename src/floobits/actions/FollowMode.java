package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FlooHandler;

public class FollowMode extends IsJoinedAction {
    public void actionPerformed(AnActionEvent e, FlooHandler flooHandler) {
        boolean mode = !flooHandler.stalking;
        flooHandler.status_message(String.format("%s follow mode", mode ? "Enabling" : "Disabling"));
        e.getPresentation().setText(String.format("%s follow mode", mode ? "Disable" : "Enable"));
        flooHandler.stalking = mode;
    }
}
