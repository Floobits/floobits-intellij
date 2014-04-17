package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.hash.HashSet;
import floobits.BaseContext;
import floobits.FloobitsPlugin;
import floobits.common.handlers.FlooHandler;
import floobits.common.Utils;

import java.util.ArrayList;

public class AddToWorkspace extends IsJoinedAction {
    public void actionPerformed(AnActionEvent e, FlooHandler flooHandler) {
        final HashSet<VirtualFile> filesToAdd = new HashSet<VirtualFile>();
        final VirtualFile[] virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());

        if (virtualFiles == null) {
            return;
        }
        BaseContext context = FloobitsPlugin.getInstance(e.getProject()).context;
        for (final VirtualFile virtualFile : virtualFiles) {
            if (filesToAdd.contains(virtualFile)) {
                continue;
            }
            if (!Utils.isSharable(virtualFile)) {
                continue;
            }
            if (!context.isShared(virtualFile.getPath())) {
                continue;
            }
            ArrayList<VirtualFile> allValidNestedFiles = Utils.getAllValidNestedFiles(context, virtualFile);
            filesToAdd.addAll(allValidNestedFiles);
        }

        for (VirtualFile virtualFile : filesToAdd) {
            flooHandler.upload(virtualFile);
        }
    }
}
