package floobits.common.interfaces;

import java.io.File;
import java.util.HashSet;

public interface IFactory {
    IFile findFileByIoFile(File file);
    IFile createFile(String path);
    IDoc getDocument(IFile file);
    IDoc getDocument(String relPath);
    IFile createDirectories(String path);
    IFile findFileByPath(String path);
    IFile getOrCreateFile(String path);
    void removeHighlightsForUser(int userID);
    void removeHighlight(Integer userId, final String path);
    boolean openFile(File file);
    void clearHighlights();
    void clearReadOnlyState();
    void goToLastHighlight();
    HashSet<String> readOnlyBufferIds = new HashSet<String>();
}
