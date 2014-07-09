package floobits.common.interfaces;

import floobits.FlooContext;

import java.io.File;
import java.util.HashSet;

/**
 * Created by kans on 7/7/14.
 */
public interface VFactory {
    public VFile findFileByIoFile(File file);
    public VDoc getDocument(String path);
    public VFile createDirectories(String path);
    public VFile findFileByPath(String path);
    public void removeHighlightsForUser(int userID);
    public void removeHighlight(Integer userId, final String path);
    public void removeHighlight(Integer userId, final String path, final VDoc document);
    public void clearHighlights();
    public void clearReadOnlyState();
    public boolean openFile(FlooContext context, File file)
    public HashSet<String> readOnlyBufferIds = new HashSet<String>();
}
