package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.common.EditorEventHandler;

public class OpenWorkspaceInBrowser extends IsJoinedAction {
    @Override
    public void actionPerformed(AnActionEvent e, EditorEventHandler editorEventHandler) {
        editorEventHandler.openInBrowser();
    }
}
