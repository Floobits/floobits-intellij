package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.hash.HashSet;
import floobits.FloobitsPlugin;
import floobits.utilities.IntelliUtils;
import floobits.common.EditorEventHandler;
import floobits.common.interfaces.IFile;
import floobits.impl.ImpContext;
import floobits.impl.ImpFile;

import java.util.ArrayList;

public class AddToWorkspace extends IsJoinedAction {
    public void actionPerformed(AnActionEvent e, EditorEventHandler editorEventHandler) {
        final HashSet<IFile> filesToAdd = new HashSet<IFile>();
        final VirtualFile[] virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());

        if (virtualFiles == null) {
            return;
        }
        ImpContext context = FloobitsPlugin.getInstance(e.getProject()).context;
        for (final VirtualFile virtualFile : virtualFiles) {
            ImpFile impFile = new ImpFile(virtualFile);
            if (filesToAdd.contains(impFile)) {
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
            if (context.isIgnored(impFile)) {
                context.statusMessage(String.format("Skipped adding %s because it is ignored.", virtualFile.getPath()));
                continue;
            }

            ArrayList<IFile> allValidNestedFiles = IntelliUtils.getAllValidNestedFiles(context, virtualFile);
            filesToAdd.addAll(allValidNestedFiles);
        }

        for (IFile virtualFile : filesToAdd) {
            editorEventHandler.upload(virtualFile);
        }
    }
}
