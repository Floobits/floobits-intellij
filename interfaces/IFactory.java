package floobits.common.interfaces;

import java.io.File;
import java.util.HashSet;

/**
 * Created by kans on 7/7/14.
 */
public interface IFactory {
    public IFile findFileByIoFile(File file);
    public IDoc getDocument(IFile file);
    public IDoc getDocument(String relPath);
    public IFile createDirectories(String path);
    public IFile findFileByPath(String path);
    public void removeHighlightsForUser(int userID);
    public void removeHighlight(Integer userId, final String path);
    boolean openFile(File file);
    public void clearHighlights();
    public void clearReadOnlyState();
    public HashSet<String> readOnlyBufferIds = new HashSet<String>();
}
