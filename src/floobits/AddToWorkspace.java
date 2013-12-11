package floobits;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.util.containers.hash.HashSet;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;

public class AddToWorkspace extends AnAction {
    public void actionPerformed(AnActionEvent e) {
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
                    return add(file, filesToAdd);
                }

            });
        }

        Ignore ignore;
        FlooHandler flooHandler = FlooHandler.getInstance();

        if (filesToAdd.size() <= 1) {
            VirtualFile toAdd = ((VirtualFile[])filesToAdd.toArray())[0];
            if (!Ignore.is_ignored(toAdd.getPath(), null)) {
                flooHandler.upload(toAdd);
            }
            return;
        }

        try {
            ignore = new Ignore(new File(Shared.colabDir), null, false);
        } catch (Exception ex) {
            Flog.error(ex);
            return;
        }

        for (VirtualFile virtualFile : filesToAdd) {
            if (!ignore.isIgnored(virtualFile.getCanonicalPath())) {
               flooHandler.upload(virtualFile);
            }
        }
    }

    private boolean add(VirtualFile virtualFile, HashSet<VirtualFile> set) {
        boolean we_donut_care = !virtualFile.isInLocalFileSystem()
                || virtualFile.isSpecialFile()
                || virtualFile.isSymLink()
                || !Utils.isShared(virtualFile.getCanonicalPath());
        if (we_donut_care) {
            return false;
        }
        if (set.contains(virtualFile)) {
            return false;
        }
        if (virtualFile.isDirectory()) {
            return true;
        }
        set.add(virtualFile);
        return true;
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
