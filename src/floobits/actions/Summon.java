package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import floobits.common.EditorEventHandler;
import floobits.utilities.GetPath;


public class Summon extends IsJoinedAction {

    @Override
    public void actionPerformed(AnActionEvent e, final EditorEventHandler editorEventHandler) {
        final Editor editor = PlatformDataKeys.EDITOR.getData(e.getDataContext());
        if (editor == null) {
            return;
        }
        Document document = editor.getDocument();
        GetPath.getPath(new GetPath(document) {
            @Override
            public void if_path(String path) {
                int offset = editor.getCaretModel().getOffset();
                editorEventHandler.summon(path, offset);
            }
        });
    }
}
