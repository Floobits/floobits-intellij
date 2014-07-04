package floobits;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.common.*;
import floobits.utilities.Flog;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;


public class BufHelper {
    public static VirtualFile createFile(FlooContext context, String path) {
        File file = new File(context.absPath(path));
        String name = file.getName();
        String parentPath = file.getParent();
        try {
            VfsUtil.createDirectories(parentPath);
        } catch (IOException e) {
            Flog.warn("createFile error %s", e);
            Utils.errorMessage("The Floobits plugin was unable to create a file.", context.project);
            return null;
        }
        VirtualFile parent = LocalFileSystem.getInstance().findFileByPath(parentPath);
        if (parent == null) {
            Flog.warn("Virtual file is null? %s", parentPath);
            return null;
        }
        VirtualFile newFile;
        try {
            newFile = parent.findOrCreateChildData(context, name);
        } catch (IOException e) {
            Flog.warn("Create file error %s", e);
            Utils.errorMessage("The Floobits plugin was unable to create a file.", context.project);
            return null;
        }
        return newFile;
    }

    public static Document getDocumentForVirtualFile(VirtualFile virtualFile) {
        if (virtualFile == null) {
            return null;
        }
        return FileDocumentManager.getInstance().getDocument(virtualFile);
    }

    public static VirtualFile getVirtualFile(FlooContext context, String path) {
        return LocalFileSystem.getInstance().findFileByPath(context.absPath(path));
    }

    public static Buf createBuf(String path, Integer id, Encoding enc, String md5, FlooContext context, OutboundRequestHandler outbound) {
        if (enc == Encoding.BASE64) {
            return new IntellijBinaryBuf(path, id, null, md5, context, outbound);
        }
        return new IntellijTextBuf(path, id, null, md5, context, outbound);
    }

    public static Buf createBuf(VirtualFile f, FlooContext context, OutboundRequestHandler outbound) {
        try {
            byte[] originalBytes = f.contentsToByteArray();
            String encodedContents = new String(originalBytes, "UTF-8");
            byte[] decodedContents = encodedContents.getBytes();
            String filePath = context.toProjectRelPath(f.getPath());
            if (Arrays.equals(decodedContents, originalBytes)) {
                Document document = FileDocumentManager.getInstance().getDocument(f);
                String contents = document == null ? encodedContents : document.getText();
                String md5 = DigestUtils.md5Hex(contents);
                return new IntellijTextBuf(filePath, null, contents, md5, context, outbound);
            } else {
                String md5 = DigestUtils.md5Hex(originalBytes);
                return new IntellijBinaryBuf(filePath, null, originalBytes, md5, context, outbound);
            }
        } catch (IOException e) {
            Flog.warn("Error getting virtual file contents in createBuf %s", f);
        }
        return null;
    }
}
