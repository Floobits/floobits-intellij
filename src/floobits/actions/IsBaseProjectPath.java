package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.utilities.Flog;

public abstract class IsBaseProjectPath extends CanFloobits {

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        if (file == null) {
            return;
        }
        String place = e.getPlace();
        e.getPresentation().setEnabled(place.equals("MainMenu") || file.getPath().equals(project.getBasePath()));
        e.getPresentation().setVisible(place.equals("MainMenu") || file.getPath().equals(project.getBasePath()));
        Flog.debug("file is %s at %s", file.getPath(), place);
    }
}
