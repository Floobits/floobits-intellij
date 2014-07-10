package floobits.impl;

import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.common.interfaces.IFile;
import floobits.utilities.Flog;

import java.io.IOException;
import java.io.InputStream;


public class ImpFile extends IFile {
    final protected VirtualFile virtualFile;

    public ImpFile(VirtualFile virtualFile) {
        this.virtualFile = virtualFile;
    }

    @Override
    public String getPath() {
        return virtualFile.getPath();
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
    public IFile makeFile(String name) {
        VirtualFile childDirectory;
        try {
            childDirectory = virtualFile.createChildDirectory(this, name);
        } catch (IOException e) {
            Flog.warn(e);
            return null;
        }
        return new ImpFile(childDirectory);
    }

    @Override
    public boolean move(Object obj, IFile d) {
        try {
            virtualFile.move(obj, ((ImpFile)d).virtualFile);
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
    public IFile[] getChildren() {
        VirtualFile[] children = virtualFile.getChildren();
        if (children == null) {
            return new IFile[]{};
        }
        IFile[] iFiles = new IFile[children.length];
        for (int i = 0; i < children.length; i++) {
            iFiles[i] = new ImpFile(children[i]);
        }
        return iFiles;
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
