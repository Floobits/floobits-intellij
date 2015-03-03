package floobits.common;

import floobits.common.interfaces.IContext;

import java.util.ArrayList;

public class HighlightContext {
    public ArrayList<ArrayList<Integer>> ranges;
    public int userid;
    public int textLength = 0;
    public String username;
    public String gravatar;
    public String path;
    public Boolean following;
    public Boolean force;
    public IContext context;
}
