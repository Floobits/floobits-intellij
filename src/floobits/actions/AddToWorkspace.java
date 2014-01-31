package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.util.containers.hash.HashSet;
import floobits.handlers.FlooHandler;
import floobits.common.Ignore;
import floobits.common.Utils;
import floobits.utilities.Flog;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class AddToWorkspace extends IsJoinedAction {
    public void actionPerformed(AnActionEvent e, FlooHandler flooHandler) {
        final HashSet<VirtualFile> filesToAdd = new HashSet<VirtualFile>();
        final VirtualFile[] virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());

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

        if (filesToAdd.size() == 1) {
            VirtualFile[] toAdds = new VirtualFile[]{};
            VirtualFile toAdd = filesToAdd.toArray(toAdds)[0];
            if (!Ignore.isIgnored(toAdd)) {
                flooHandler.upload(toAdd);
            }
            return;
        }
        Ignore ignore = Ignore.buildIgnoreTree();
        if (ignore == null) {
            Flog.throwAHorribleBlinkingErrorAtTheUser("Your file system is broken.");
            return;
        }

        for (VirtualFile virtualFile : filesToAdd) {
            if (!ignore.isIgnored(virtualFile.getPath())) {
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

    @Override
    public void update(AnActionEvent e) {
        super.update(e);    //To change body of overridden methods use File | Settings | File Templates.
        if (FlooHandler.getInstance() == null) {
            e.getPresentation().setEnabled(false);
        } else {
            e.getPresentation().setEnabled(true);
        }
    }
}
