package floobits.common;

import floobits.FlooContext;
import floobits.common.protocol.receive.FlooHighlight;

/**
 * Created by kans on 7/7/14.
 */
abstract public class VDoc {
    public abstract void removeHighlight(FlooContext context, Object obj);
    public abstract void applyHighlight(String username, FlooContext context, Boolean force, FlooHighlight highlight);
    public abstract void save(FlooContext context);
    public abstract String getText();
}
