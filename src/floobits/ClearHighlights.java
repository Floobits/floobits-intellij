package floobits;

import com.intellij.openapi.actionSystem.AnActionEvent;

public class ClearHighlights extends IsJoinedAction {
    @Override
    public void actionPerformed(AnActionEvent e, FlooHandler flooHandler) {
//        VirtualFile[] virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
        flooHandler.clear_highlights();
    }
}
