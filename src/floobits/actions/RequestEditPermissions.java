package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FloobitsPlugin;
import floobits.handlers.FlooHandler;

public class RequestEditPermissions extends IsJoinedAction {
    @Override
    public void actionPerformed(AnActionEvent e, FlooHandler flooHandler) {
        flooHandler.sendEditRequest();
    }

    protected boolean isVisible(FloobitsPlugin floobitsPlugin) {
        FlooHandler flooHandler = floobitsPlugin.context.getFlooHandler();
        return flooHandler != null && !flooHandler.can("patch") && flooHandler.can("request_perms");

    }
}
