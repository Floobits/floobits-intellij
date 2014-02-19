package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FloobitsPlugin;

public class ShareProject extends CanFloobits {

    public void actionPerformed(AnActionEvent e) {
        FloobitsPlugin.getInstance(e.getProject()).context.shareProject(false);
    }
}
