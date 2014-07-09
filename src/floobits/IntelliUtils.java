package floobits;

import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.common.interfaces.FlooContext;
import floobits.common.interfaces.VFile;
import floobits.impl.IntellijFile;

import java.util.ArrayList;


public class IntelliUtils {
    public static ArrayList<String> getAllNestedFilePaths(VirtualFile vFile) {
        final ArrayList<String> filePaths = new ArrayList<String>();
        if (!vFile.isDirectory()) {
            filePaths.add(vFile.getPath());
            return filePaths;
        }
        VfsUtil.iterateChildrenRecursively(vFile, null, new ContentIterator() {
            @Override
            public boolean processFile(VirtualFile file) {
                if (!file.isDirectory()) {
                    filePaths.add(file.getPath());
                }
                return true;
            }
        });
        return filePaths;
    }

    public static ArrayList<VFile> getAllValidNestedFiles(final FlooContext context, VirtualFile vFile) {
        final ArrayList<VFile> virtualFiles = new ArrayList<VFile>();
        IntellijFile intellijFile = new IntellijFile(vFile);

        if (!intellijFile.isDirectory()) {
            if (intellijFile.isValid() && !context.isIgnored(intellijFile)) virtualFiles.add(intellijFile);
            return virtualFiles;
        }

        VfsUtil.iterateChildrenRecursively(vFile, null, new ContentIterator() {
            @Override
            public boolean processFile(VirtualFile file) {
                IntellijFile intellijFile = new IntellijFile(file);
                if (!context.isIgnored(intellijFile) && !intellijFile.isDirectory() && intellijFile.isValid()) {
                    virtualFiles.add(intellijFile);
                }
                return true;
            }
        });
        return virtualFiles;
    }
}
