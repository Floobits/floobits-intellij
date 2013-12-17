package floobits;

import com.intellij.openapi.actionSystem.AnActionEvent;

public class LeaveWorkspace extends IsJoinedAction {
    @Override
    public void actionPerformed(AnActionEvent e, FlooHandler flooHandler) {
        flooHandler.shut_down();
    }
}
