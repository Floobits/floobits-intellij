package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.hash.HashSet;
import floobits.FlooContext;
import floobits.FloobitsPlugin;
import floobits.common.EditorEventHandler;
import floobits.common.Utils;

import java.util.ArrayList;

public class AddToWorkspace extends IsJoinedAction {
    public void actionPerformed(AnActionEvent e, EditorEventHandler editorEventHandler) {
        final HashSet<VirtualFile> filesToAdd = new HashSet<VirtualFile>();
        final VirtualFile[] virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());

        if (virtualFiles == null) {
            return;
        }
        FlooContext context = FloobitsPlugin.getInstance(e.getProject()).context;
        for (final VirtualFile virtualFile : virtualFiles) {
            if (filesToAdd.contains(virtualFile)) {
                continue;
            }
            if (!Utils.isSharable(virtualFile)) {
                context.statusMessage(String.format("Skipped adding %s because it is a special file.", virtualFile.getPath()), false);
                continue;
            }
            if (!context.isShared(virtualFile.getPath())) {
                context.statusMessage(String.format("Skipped adding %s because it is not in %s.", virtualFile.getPath(), context.colabDir), false);
                continue;
            }
            if (context.isIgnored(virtualFile)) {
                context.statusMessage(String.format("Skipped adding %s because it is ignored.", virtualFile.getPath()), false);
                continue;
            }

        ArrayList<VirtualFile> allValidNestedFiles = Utils.getAllValidNestedFiles(context, virtualFile);
            filesToAdd.addAll(allValidNestedFiles);
        }

        for (VirtualFile virtualFile : filesToAdd) {
            editorEventHandler.upload(virtualFile);
        }
    }
}
