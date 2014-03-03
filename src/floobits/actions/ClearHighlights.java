package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.handlers.FlooHandler;

public class ClearHighlights extends IsJoinedAction {
    @Override
    public void actionPerformed(AnActionEvent e, FlooHandler flooHandler) {
        flooHandler.clearHighlights();
    }
}
