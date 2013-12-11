package floobits;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;

public class ClearHighlights extends IsJoinedAction {
    @Override
    public void actionPerformed(AnActionEvent e, FlooHandler flooHandler) {
        VirtualFile[] virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
        Editor editor = PlatformDataKeys.EDITOR_EVEN_IF_INACTIVE.getData(e.getDataContext());
        Document document = editor.getDocument();
        if (virtualFiles == null) {
            return;
        }
        flooHandler.clear_highlights(virtualFiles);
    }
}
