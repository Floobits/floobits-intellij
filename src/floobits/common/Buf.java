package floobits.common;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.FloobitsPlugin;
import floobits.utilities.Flog;
import floobits.common.protocol.FlooPatch;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;


public abstract class Buf <T> {
    static Pattern NEW_LINE = Pattern.compile("\\r\\n?", Pattern.DOTALL);
    public String path;
    public Integer id;
    public volatile String md5;
    public volatile T buf;
    public Encoding encoding;
    public Timeout timeout;
    public boolean forced_patch = false;
    protected Project project;

    public Buf(String path, Integer id, T buf, String md5, Project project) {
        this.id = id;
        this.path = path;
        this.buf = buf;
        this.md5 = md5;
        this.project = project;
    }

    public void cancelTimeout () {
        if (timeout != null) {
            timeout.cancel();
            timeout = null;
        }
    }
    public static boolean isBad(Buf b) {
        return (b == null || !b.isPopulated());
    }

    public Boolean isPopulated() {
        return this.id != null && this.buf != null;
    }

    public VirtualFile getVirtualFile() {
        return LocalFileSystem.getInstance().findFileByPath(Utils.absPath(this.path));
    }
    public String toString() {
        return String.format("id: %s file: %s", id, path);
    }

    public VirtualFile createFile() {
        File file = new File(Utils.absPath(path));
        String name = file.getName();
        String parentPath = file.getParent();
        try {
            VfsUtil.createDirectories(parentPath);
        } catch (IOException e) {
            Flog.warn("createFile error %s", e);
            Utils.error_message("The Floobits plugin was unable to create a file.", project);
            return null;
        }
        VirtualFile parent = LocalFileSystem.getInstance().findFileByPath(parentPath);
        if (parent == null) {
            Flog.warn("Virtual file is null? %s", parentPath);
            return null;
        }
        VirtualFile newFile;
        try {
            newFile = parent.findOrCreateChildData(FloobitsPlugin.getHandler(), name);
        } catch (IOException e) {
            Flog.warn("Create file error %s", e);
            Utils.error_message("The Floobits plugin was unable to create a file.", project);
            return null;
        }
        return newFile;
    }

    abstract public void read ();
    abstract public void write();
    abstract public void set (String s, String md5);
    abstract public void patch (FlooPatch res);
    abstract public void send_patch (VirtualFile virtualFile);
    abstract public String serialize();

    static Document getDocumentForVirtualFile(VirtualFile virtualFile) {
        if (virtualFile == null) {
            return null;
        }
        return FileDocumentManager.getInstance().getDocument(virtualFile);
    }
    public static Buf createBuf(String path, Integer id, Encoding enc, String md5, Project project) {
        if (enc == Encoding.BASE64) {
            return new BinaryBuf(path, id, null, md5, project);
        }
        return new TextBuf(path, id, null, md5, project);
    }
    public static Buf createBuf(VirtualFile virtualFile, Project project) {
        try {
            byte[] originalBytes = virtualFile.contentsToByteArray();
            String encodedContents = new String(originalBytes, "UTF-8");
            byte[] decodedContents = encodedContents.getBytes();
            String filePath = Utils.toProjectRelPath(virtualFile.getPath());
            if (Arrays.equals(decodedContents, originalBytes)) {
                String md5 = DigestUtils.md5Hex(encodedContents);
                return new TextBuf(filePath, null, encodedContents, md5, project);
            } else {
                String md5 = DigestUtils.md5Hex(originalBytes);
                return new BinaryBuf(filePath, null, originalBytes, md5, project);
            }
        } catch (IOException e) {
            Flog.warn("Error getting virtual file contents in createBuf %s", virtualFile);
        }
        return null;
    }
}

