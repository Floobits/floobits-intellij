package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FloobitsPlugin;
import floobits.utilities.Flog;

/**
 * User: bjorn
 * Date: 2/14/14
 * Time: 12:08 PM
 */
public class ShareProjectPrivately extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        FloobitsPlugin.getInstance(e.getProject()).context.shareProject(true);
    }
}
