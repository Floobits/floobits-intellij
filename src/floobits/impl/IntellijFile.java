package floobits.impl;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.common.interfaces.FlooContext;
import floobits.common.interfaces.VDoc;
import floobits.common.interfaces.VFile;
import floobits.utilities.Flog;

import java.io.IOException;
import java.io.InputStream;


public class IntellijFile extends VFile {
    final protected VirtualFile virtualFile;

    public IntellijFile(VirtualFile virtualFile) {
        this.virtualFile = virtualFile;
    }

    @Override
    public String getPath() {
        return virtualFile.getPath();
    }

    @Override
    public VDoc getDocument(FlooContext context) {
        Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (document == null) {
            return null;
        }
        return new IntellijDoc(context, document);
    }

    @Override
    public byte[] getBytes() {
        try {
            return virtualFile.contentsToByteArray();
        } catch (IOException e) {
            return null;
        }
    }


    @Override
    public boolean setBytes(byte[] bytes) {
        try {
            virtualFile.setBinaryContent(bytes);
        } catch (IOException e) {
            Flog.warn(e);
            return false;
        }
        return true;
    }

    @Override
    public boolean rename(Object obj, String name) {
        try {
            virtualFile.rename(obj, name);
        } catch (IOException e) {
            Flog.warn(e);
            return false;
        }
        return true;
    }

    @Override
    public boolean createDirectories(String dir) {
        try {
            virtualFile.createChildDirectory(this, dir);
        } catch (IOException e) {
            Flog.warn(e);
            return false;
        }
        return true;
    }

    @Override
    public VFile makeFile(String name) {
        VirtualFile childDirectory;
        try {
            childDirectory = virtualFile.createChildDirectory(this, name);
        } catch (IOException e) {
            Flog.warn(e);
            return null;
        }
        return new IntellijFile(childDirectory);
    }

    @Override
    public boolean move(Object obj, VFile d) {
        try {
            virtualFile.move(obj, ((IntellijFile)d).virtualFile);
        } catch (Throwable e) {
            Flog.warn(e);
            return false;
        }
        return true;
    }

    @Override
    public boolean delete(Object obj) {
        try {
            virtualFile.delete(obj);
        } catch (Throwable e) {
            Flog.warn(e);
            return false;
        }
        return true;
    }

    @Override
    public boolean isDirectory() {
        return virtualFile.isDirectory();
    }

    @Override
    public VFile[] getChildren() {
        VirtualFile[] children = virtualFile.getChildren();
        VFile[] vFiles = new VFile[children.length];
        for (int i = 0; i < children.length; i++) {
            vFiles[i] = new IntellijFile(virtualFile);
        }
        return vFiles;
    }

    @Override
    public InputStream getInputStream() {
        try {
            return virtualFile.getInputStream();
        } catch (IOException e) {
            Flog.warn(e);
            return null;
        }
    }

    @Override
    public String getName() {
        return virtualFile.getName();
    }

    @Override
    public boolean isValid() {
        return virtualFile.isValid();
    }

    @Override
    public long getLength() {
        return virtualFile.getLength();
    }

    @Override
    public boolean isSpecial() {
        return virtualFile.is(VFileProperty.SPECIAL);
    }

    @Override
    public boolean isSymLink() {
        return virtualFile.is(VFileProperty.SYMLINK);
    }

    @Override
    public void refresh() {
        virtualFile.refresh(false, false);
    }

    @Override
    public boolean exists() {
        return virtualFile.exists();
    }
}
