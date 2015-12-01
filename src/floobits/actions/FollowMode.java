package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FloobitsPlugin;
import floobits.common.EditorEventHandler;
import floobits.common.protocol.handlers.FlooHandler;
import floobits.utilities.Flog;

public class FollowMode extends IsJoinedAction {
    public void actionPerformed(AnActionEvent e, EditorEventHandler editorEventHandler, FloobitsPlugin floobitsPlugin) {
        editorEventHandler.follow();
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        FloobitsPlugin floobitsPlugin;
        FlooHandler flooHandler;
        floobitsPlugin = FloobitsPlugin.getInstance(e.getProject());
        if (floobitsPlugin == null) {
            Flog.log("No floobits plugin, aborting update.");
            return;
        }
        flooHandler = floobitsPlugin.context.getFlooHandler();
        if (flooHandler == null) {
            return;
        }
        boolean mode = flooHandler.editorEventHandler.state.getFollowing();
        e.getPresentation().setText(String.format("%s follow mode", mode ? "Disable" : "Enable"));
    }
}
