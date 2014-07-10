package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.hash.HashSet;
import floobits.FloobitsPlugin;
import floobits.IntelliUtils;
import floobits.common.EditorEventHandler;
import floobits.common.interfaces.VFile;
import floobits.impl.IntelliContext;
import floobits.impl.IntellijFile;

import java.util.ArrayList;

public class AddToWorkspace extends IsJoinedAction {
    public void actionPerformed(AnActionEvent e, EditorEventHandler editorEventHandler) {
        final HashSet<VFile> filesToAdd = new HashSet<VFile>();
        final VirtualFile[] virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());

        if (virtualFiles == null) {
            return;
        }
        IntelliContext context = FloobitsPlugin.getInstance(e.getProject()).context;
        for (final VirtualFile virtualFile : virtualFiles) {
            IntellijFile intellijFile = new IntellijFile(virtualFile);
            if (filesToAdd.contains(intellijFile)) {
                continue;
            }
            if (!IntelliUtils.isSharable(virtualFile)) {
                context.statusMessage(String.format("Skipped adding %s because it is a special file.", virtualFile.getPath()));
                continue;
            }
            if (!context.isShared(virtualFile.getPath())) {
                context.statusMessage(String.format("Skipped adding %s because it is not in %s.", virtualFile.getPath(), context.colabDir));
                continue;
            }
            if (context.isIgnored(intellijFile)) {
                context.statusMessage(String.format("Skipped adding %s because it is ignored.", virtualFile.getPath()));
                continue;
            }

            ArrayList<VFile> allValidNestedFiles = IntelliUtils.getAllValidNestedFiles(context, virtualFile);
            filesToAdd.addAll(allValidNestedFiles);
        }

        for (VFile virtualFile : filesToAdd) {
            editorEventHandler.upload(virtualFile);
        }
    }
}
