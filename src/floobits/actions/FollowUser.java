package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FloobitsPlugin;
import floobits.common.EditorEventHandler;

public class FollowUser extends IsJoinedAction {
    @Override
    public void actionPerformed(AnActionEvent e, EditorEventHandler editorEventHandler) {
        FloobitsPlugin.getInstance(e.getProject()).context.followUser();
    }
}
