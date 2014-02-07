package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.util.containers.hash.HashSet;
import floobits.FloobitsPlugin;
import floobits.handlers.FlooHandler;
import floobits.common.Ignore;
import floobits.common.Utils;
import org.jetbrains.annotations.NotNull;

public class AddToWorkspace extends IsJoinedAction {
    public void actionPerformed(AnActionEvent e, FlooHandler flooHandler) {
        final HashSet<VirtualFile> filesToAdd = new HashSet<VirtualFile>();
        final VirtualFile[] virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());

        if (flooHandler == null) {
            return;
        }

        if (virtualFiles == null) {
            return;
        }

        for (final VirtualFile virtualFile : virtualFiles) {
            if (filesToAdd.contains(virtualFile)) {
                continue;
            }
            if (!virtualFile.isDirectory()) {
                add(virtualFile, filesToAdd);
                continue;
            }

            VfsUtilCore.visitChildrenRecursively(virtualFile, new VirtualFileVisitor() {
                @Override
                public boolean visitFile(@NotNull VirtualFile file) {
                    add(file, filesToAdd);
                    return true;
                }
            });
        }

        for (VirtualFile virtualFile : filesToAdd) {
            if (!flooHandler.context.isIgnored(virtualFile)) {
               flooHandler.upload(virtualFile);
            }
        }
    }

    private void add(VirtualFile virtualFile, HashSet<VirtualFile> set) {
        if (!Utils.isSharableFile(virtualFile)) {
            return;
        }
        if (set.contains(virtualFile)) {
            return;
        }
        if (virtualFile.isDirectory()) {
            return;
        }
        set.add(virtualFile);
    }
}
