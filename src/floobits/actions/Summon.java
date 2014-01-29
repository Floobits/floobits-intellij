package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import floobits.handlers.FlooHandler;
import floobits.utilities.GetPath;


public class Summon extends IsJoinedAction {
    public void actionPerformed(final AnActionEvent e) {
        final Editor editor = PlatformDataKeys.EDITOR.getData(e.getDataContext());
        if (editor == null) {
            return;
        }
        Document document = editor.getDocument();

        GetPath getPath = new GetPath(document) {
            @Override
            public void if_path(String path, FlooHandler flooHandler) {
                int offset = editor.getCaretModel().getOffset();
                flooHandler.send_summon(path, offset);
            }
        };
        GetPath.getPath(getPath);
    }
}
