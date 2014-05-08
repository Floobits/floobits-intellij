package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.common.EditorEventHandler;

public class ClearHighlights extends IsJoinedAction {
    @Override
    public void actionPerformed(AnActionEvent e, EditorEventHandler editorEventHandler) {
        editorEventHandler.clearHighlights();
    }
}
