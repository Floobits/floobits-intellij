package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.FloobitsPlugin;
import floobits.utilities.IntelliUtils;
import floobits.common.EditorEventHandler;
import floobits.common.interfaces.IContext;

import java.util.ArrayList;
import java.util.HashSet;

public class DeleteFromWorkspace extends IsJoinedAction {
    @Override
    public void actionPerformed(AnActionEvent e, EditorEventHandler editorEventHandler) {
        final HashSet<String> fileHashSet = new HashSet<String>();
        final VirtualFile[] virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());

        if (editorEventHandler == null || virtualFiles == null) {
            return;
        }
        IContext context = FloobitsPlugin.getInstance(e.getProject()).context;
        for (final VirtualFile virtualFile : virtualFiles) {
            if (fileHashSet.contains(virtualFile.getPath())) {
                continue;
            }
            if (!context.isShared(virtualFile.getPath())) {
                continue;
            }
            ArrayList<String> allNestedFilePaths = IntelliUtils.getAllNestedFilePaths(virtualFile);
            fileHashSet.addAll(allNestedFilePaths);
        }

        editorEventHandler.softDelete(fileHashSet);
    }
}
