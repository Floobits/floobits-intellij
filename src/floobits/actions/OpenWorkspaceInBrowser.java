package floobits.actions;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.FloobitsPlugin;
import floobits.common.BrowserOpener;
import floobits.common.DotFloo;
import floobits.common.FlooUrl;
import floobits.common.Utils;
import floobits.impl.ContextImpl;
import floobits.utilities.Flog;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;

public class OpenWorkspaceInBrowser extends CanFloobits {
    @Override
        public void actionPerformed(AnActionEvent event, Project project, FloobitsPlugin plugin, ContextImpl context) {
        String projectPath = project.getBasePath();
        int line = 0;
        String path = null;
        Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        Editor editor = PlatformDataKeys.EDITOR.getData(DataManager.getInstance().getDataContext(owner));
        if (editor != null) {
            Document document = editor.getDocument();
            VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
            CaretModel caretModel = editor.getCaretModel();
            LogicalPosition logicalPosition = caretModel.getLogicalPosition();
            line = logicalPosition.line + 1;
            String absPath = virtualFile.getPath();
            path = Utils.toProjectRelPath(absPath, projectPath);
        }
        FlooUrl flooUrl = DotFloo.read(projectPath);
        if (flooUrl == null) {
            context.errorMessage(String.format("Could not determine the Floobits workspace for %s, did you create it and join it previously?", path));
            return;
        }
        String urlStr = flooUrl.toString();
        if (path != null) {
            urlStr = String.format("%s/file/%s:%d", flooUrl, path, line);
        }
        URI uri;
        try {
            uri = new URI(urlStr);
        } catch (URISyntaxException e) {
            Flog.info("Couldn't open settings in browser", e);
            return;
        }
        BrowserOpener.getInstance().openInBrowser(uri, "Click here to go your project's settings.", context);
    }

}
