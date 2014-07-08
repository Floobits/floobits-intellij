package floobits.common;

import java.io.File;

/**
 * Created by kans on 7/7/14.
 */
public interface VFactory {
    public VFile findFileByPath(String path);
    public VFile findFileByIoFile(File file);
    public VDoc getDocument(String path);
    public boolean createDirectories(String path);
}
