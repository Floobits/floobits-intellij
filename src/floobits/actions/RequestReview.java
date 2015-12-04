package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;
import floobits.common.EditorEventHandler;
import floobits.common.interfaces.IContext;
import floobits.dialogs.RequestCodeReviewDialog;


public class RequestReview  extends IsJoinedAction {
    @Override
    public void actionPerformed(AnActionEvent e, EditorEventHandler editorEventHandler, FloobitsPlugin floobitsPlugin) {
//        IContext context = null;
//        if (floobitsPlugin != null) {
//            context = floobitsPlugin.context;
//        }
        Project project = e.getProject();
        RequestCodeReviewDialog dialog = new RequestCodeReviewDialog(project, true);
        dialog.show();
    }
}