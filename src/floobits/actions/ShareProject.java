package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import floobits.FloobitsApplication;
import floobits.FloobitsPlugin;

public class ShareProject extends AnAction {

    public void actionPerformed(AnActionEvent e) {
        FloobitsPlugin.getInstance(e.getProject()).context.shareProject();
    }
}
