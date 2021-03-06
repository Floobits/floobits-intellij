package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;
import floobits.impl.ContextImpl;
import floobits.utilities.Flog;


abstract public class CanFloobits extends AnAction {

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        e.getPresentation().setEnabled(true);
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            Flog.warn("Tried to perform an action but there is no project");
            return;
        }
        final FloobitsPlugin floobitsPlugin = project.getService(FloobitsPlugin.class);
        floobitsPlugin.setupAccount(new Runnable() {
            @Override
            public void run() {
                actionPerformed(e, project, floobitsPlugin, floobitsPlugin.context);
            }
        });
    }

    abstract public void actionPerformed(AnActionEvent e, Project project, FloobitsPlugin plugin, ContextImpl context);
}
