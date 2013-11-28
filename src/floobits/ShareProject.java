package floobits;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class ShareProject extends AnAction {

    public void actionPerformed(AnActionEvent e) {
        FloobitsPlugin.shareProject(e.getProject());
    }
}
