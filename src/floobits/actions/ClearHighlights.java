package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FlooHandler;
import floobits.actions.IsJoinedAction;

public class ClearHighlights extends IsJoinedAction {
    @Override
    public void actionPerformed(AnActionEvent e, FlooHandler flooHandler) {
//        VirtualFile[] virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
        flooHandler.clear_highlights();
    }
}
