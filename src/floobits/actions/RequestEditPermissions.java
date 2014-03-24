package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FloobitsPlugin;
import floobits.common.handlers.FlooHandler;

public class RequestEditPermissions extends IsJoinedAction {
    @Override
    public void actionPerformed(AnActionEvent e, FlooHandler flooHandler) {
        flooHandler.sendEditRequest();
    }

    protected boolean isEnabled(FloobitsPlugin floobitsPlugin) {
        if (!super.isEnabled(floobitsPlugin)) {
            return false;
        }
        FlooHandler flooHandler = floobitsPlugin.context.getFlooHandler();
        return flooHandler != null && !flooHandler.can("patch") && flooHandler.can("request_perms");

    }
}
