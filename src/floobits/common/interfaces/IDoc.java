package floobits.common.interfaces;

import floobits.common.HighlightContext;
import floobits.common.dmp.FlooPatchPosition;


abstract public class IDoc {
    public abstract void removeHighlight(Integer userId, final String path);
    public abstract void applyHighlight(HighlightContext highlight);
    public abstract void save();
    public abstract String getText();
    public abstract void setText(String text);
    public abstract void setReadOnly(boolean readOnly);
    public abstract boolean makeWritable();
    public abstract IFile getVirtualFile();
    public abstract String patch(FlooPatchPosition[] positions);
}
