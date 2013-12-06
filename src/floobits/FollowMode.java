package floobits;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class FollowMode extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        FlooHandler instance = FlooHandler.getInstance();
        if (instance == null) {
            return;
        }
        instance.stalking = !instance.stalking;
    }
}
