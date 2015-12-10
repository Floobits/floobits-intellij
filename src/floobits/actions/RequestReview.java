package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import floobits.FloobitsPlugin;
import floobits.common.EditorEventHandler;
import floobits.common.FlooUrl;
import floobits.common.interfaces.IContext;
import floobits.common.protocol.handlers.FlooHandler;
import floobits.dialogs.RequestCodeReviewDialog;


public class RequestReview  extends IsJoinedAction {
    @Override
    public void actionPerformed(AnActionEvent e, EditorEventHandler editorEventHandler, FloobitsPlugin floobitsPlugin) {
        IContext context = null;
        if (floobitsPlugin != null) {
            context = floobitsPlugin.context;
        }
        if (context == null) {
            return;
        }
        Project project = e.getProject();
        FlooHandler flooHandler = context.getFlooHandler();
        assert flooHandler != null;
        FlooUrl flooUrl = flooHandler.getUrl();
        RequestCodeReviewDialog dialog = new RequestCodeReviewDialog(flooUrl, context, project);
        dialog.show();
    }
}