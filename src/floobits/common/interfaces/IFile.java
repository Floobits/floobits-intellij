package floobits.common.interfaces;

import java.io.InputStream;

/**
 * Created by kans on 7/7/14.
 */
public abstract class IFile {
    public abstract String getPath();
    public abstract boolean rename(Object obj, String name);
    public abstract IFile makeFile(String name);
    public abstract boolean move(Object obj, IFile d);
    public abstract boolean delete(Object obj);
    public abstract IFile[] getChildren();
    public abstract String getName();
    public abstract long getLength();
    public abstract boolean exists();
    public abstract boolean isDirectory();
    public abstract boolean isSpecial();
    public abstract boolean isSymLink();
    public abstract boolean isValid();
    public abstract byte[] getBytes();
    public abstract boolean setBytes(byte[] bytes);
    public abstract void refresh();
    public abstract boolean createDirectories(String dir);
    public abstract InputStream getInputStream();

}
