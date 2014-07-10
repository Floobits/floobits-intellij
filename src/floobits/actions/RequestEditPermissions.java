package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FloobitsPlugin;
import floobits.common.EditorEventHandler;
import floobits.common.protocol.handlers.FlooHandler;

public class RequestEditPermissions extends IsJoinedAction {
    @Override
    public void actionPerformed(AnActionEvent e, EditorEventHandler editorEventHandler) {
        editorEventHandler.sendEditRequest();
    }

    protected boolean isEnabled(FloobitsPlugin floobitsPlugin) {
        if (!super.isEnabled(floobitsPlugin)) {
            return false;
        }
        FlooHandler flooHandler = floobitsPlugin.context.getFlooHandler();
        return flooHandler != null && !flooHandler.state.can("patch") && flooHandler.state.can("request_perms");

    }
}
