package floobits.common.interfaces;

import floobits.common.dmp.FlooPatchPosition;

import java.util.ArrayList;


abstract public class IDoc {
    public abstract void removeHighlight(Integer userId, final String path);
    public abstract void applyHighlight(String path, int userID, String username, Boolean following, Boolean force, ArrayList<ArrayList<Integer>> ranges,
                                        String gravatar);
    public abstract void save();
    public abstract String getText();
    public abstract void setText(String text);
    public abstract void setReadOnly(boolean readOnly);
    public abstract boolean makeWritable();
    public abstract IFile getVirtualFile();
    public abstract String patch(FlooPatchPosition[] positions);
}
