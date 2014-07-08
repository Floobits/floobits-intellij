package floobits.Virtual;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.FlooContext;
import floobits.common.VDoc;
import floobits.common.VFactory;
import floobits.common.VFile;

import java.io.File;

/**
 * Created by kans on 7/7/14.
 */
public class IntellijVFactory implements VFactory {

    public IntellijVFactory() {

    }

    @Override
    public VFile findFileByPath(FlooContext context, String path) {
        LocalFileSystem instance = LocalFileSystem.getInstance();
        VirtualFile fileByPath = instance.findFileByPath(context.absPath(path));
        if (fileByPath != null && fileByPath.isValid()) {
            return new IntellijFile(fileByPath);
        }
        return null;
    }

    @Override
    public VFile findFileByIoFile(File file) {
        VirtualFile fileByIoFile = VfsUtil.findFileByIoFile(file, true);
        if (fileByIoFile == null) {
            return null;
        }
        return new IntellijFile(fileByIoFile);
    }

    @Override
    public VDoc getDocument(String path) {
        return null;
    }

    @Override
    public boolean createDirectories(String path) {
        return false;
    }

    public boolean openFile(FlooContext context, File file) {
        VirtualFile floorc = LocalFileSystem.getInstance().findFileByIoFile(file);
        if (floorc == null) {
            return false;
        }
        FileEditorManager.getInstance(context.project).openFile(floorc, true);
        return true;
    }
}
