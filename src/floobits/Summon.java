package floobits;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;


public class Summon extends AnAction {
    public void actionPerformed(final AnActionEvent e) {
        final Editor editor = PlatformDataKeys.EDITOR.getData(e.getDataContext());
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
