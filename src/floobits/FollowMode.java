package floobits;

import com.intellij.openapi.actionSystem.AnActionEvent;

public class FollowMode extends IsJoinedAction {
    public void actionPerformed(AnActionEvent e) {
        FlooHandler instance = FlooHandler.getInstance();
        if (instance == null) {
            return;
        }
        instance.stalking = !instance.stalking;
        instance.status_message(String.format("%s follow mode", instance.stalking ? "Enabling" : "Disabling"));
    }
}
