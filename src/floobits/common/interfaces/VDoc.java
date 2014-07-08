package floobits.common.interfaces;

import floobits.common.dmp.FlooPatchPosition;

import java.util.ArrayList;


abstract public class VDoc {
    public abstract void removeHighlight(Object obj);
    public abstract void applyHighlight(String path, int userID, String username, Boolean force, ArrayList<ArrayList<Integer>> ranges);
    public abstract void save();
    public abstract String getText();
    public abstract void setText(String text);
    public abstract void setReadOnly(boolean readOnly);
    public abstract boolean makeWritable();
    public abstract VFile getVirtualFile();
    public abstract String patch(FlooPatchPosition[] positions);
}
