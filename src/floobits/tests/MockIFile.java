package floobits.tests;

import floobits.common.interfaces.IFile;

import java.io.InputStream;

public class MockIFile extends IFile {

    public Boolean isValid = true;
    public Boolean isSpecial = false;
    public Boolean isSymlink = false;
    public Boolean isDirectory = false;
    public int length = 100;

    @Override
    public String getPath() {
        return null;
    }

    @Override
    public boolean rename(Object obj, String name) {
        return false;
    }

    @Override
    public boolean move(Object obj, IFile d) {
        return false;
    }

    @Override
    public boolean delete(Object obj) {
        return false;
    }

    @Override
    public IFile[] getChildren() {
        return new IFile[0];
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return isDirectory;
    }

    @Override
    public boolean isSpecial() {
        return isSpecial;
    }

    @Override
    public boolean isSymLink() {
        return isSymlink;
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
    public byte[] getBytes() {
        return new byte[0];
    }

    @Override
    public boolean setBytes(byte[] bytes) {
        return false;
    }

    @Override
    public void refresh() {

    }

    @Override
    public boolean createDirectories(String dir) {
        return false;
    }

    @Override
    public InputStream getInputStream() {
        return null;
    }
}
