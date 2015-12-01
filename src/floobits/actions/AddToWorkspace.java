package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.hash.HashSet;
import floobits.FloobitsPlugin;
import floobits.common.EditorEventHandler;
import floobits.common.interfaces.IFile;
import floobits.impl.ContextImpl;
import floobits.impl.FileImpl;
import floobits.utilities.IntelliUtils;

import java.util.ArrayList;

public class AddToWorkspace extends IsJoinedAction {
    public void actionPerformed(AnActionEvent e, EditorEventHandler editorEventHandler, FloobitsPlugin floobitsPlugin) {
        final HashSet<IFile> filesToAdd = new HashSet<IFile>();
        final VirtualFile[] virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());

        if (virtualFiles == null) {
            return;
        }
        ContextImpl context = floobitsPlugin.context;
        for (final VirtualFile virtualFile : virtualFiles) {
            FileImpl fileImpl = new FileImpl(virtualFile);
            if (filesToAdd.contains(fileImpl)) {
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
            if (context.isIgnored(fileImpl)) {
                context.statusMessage(String.format("Skipped adding %s because it is ignored.", virtualFile.getPath()));
                continue;
            }

            ArrayList<IFile> allValidNestedFiles = IntelliUtils.getAllValidNestedFiles(context, virtualFile);
            filesToAdd.addAll(allValidNestedFiles);
        }
        // Let's show a status message when we're uploading lots because it can lag before we get create bufs back.
        if (filesToAdd.size() > 10 ) {
            context.statusMessage(String.format("Uploading %d files.", filesToAdd.size()));
        }
        for (IFile virtualFile : filesToAdd) {
            editorEventHandler.upload(virtualFile);
        }
    }
}
