package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.FlooContext;
import floobits.FloobitsPlugin;
import floobits.common.Utils;
import floobits.common.handlers.FlooHandler;

import java.util.ArrayList;
import java.util.HashSet;

public class DeleteFromWorkspace extends IsJoinedAction {
    @Override
    public void actionPerformed(AnActionEvent e, FlooHandler flooHandler) {
        final HashSet<String> fileHashSet = new HashSet<String>();
        final VirtualFile[] virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());

        if (flooHandler == null || virtualFiles == null) {
            return;
        }
        FlooContext context = FloobitsPlugin.getInstance(e.getProject()).context;
        for (final VirtualFile virtualFile : virtualFiles) {
            if (fileHashSet.contains(virtualFile.getPath())) {
                continue;
            }
            if (!context.isShared(virtualFile.getPath())) {
                continue;
            }
            ArrayList<String> allNestedFilePaths = Utils.getAllNestedFilePaths(context, virtualFile);
            fileHashSet.addAll(allNestedFilePaths);
        }

        flooHandler.untellij_soft_delete(fileHashSet);
    }
}
