package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FloobitsPlugin;

/**
 * User: bjorn
 * Date: 2/14/14
 * Time: 12:08 PM
 */
public class ShareProjectPrivately extends CanFloobits {
    public void actionPerformed(AnActionEvent e) {
        FloobitsPlugin.getInstance(e.getProject()).context.shareProject(true);
    }
}
