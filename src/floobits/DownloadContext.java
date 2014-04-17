package floobits;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import floobits.common.FlooUrl;
import floobits.common.Timeouts;
import floobits.common.handlers.FlooHandler;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class DownloadContext extends BaseContext {
    public DownloadContext() {
        this(null);
    }
    public DownloadContext(Project project) {
        super(project);
    }

    @Override
    public void shareProject(boolean _private_) {

    }

    @Nullable
    @Override
    public FlooHandler getFlooHandler() {
        return null;
    }

    @Override
    public void loadChatManager() {

    }

    @Override
    public void createAccount() {

    }

    @Override
    public void linkEditor() {

    }

    @Override
    public void flashMessage(String message) {

    }

    @Override
    public void statusMessage(String message, NotificationType notificationType) {

    }

    @Override
    public void statusMessage(String message, boolean isChat) {

    }

    @Override
    public void errorMessage(String message) {

    }

    @Override
    public boolean openFile(File file) {
        return false;
    }

    @Override
    public void joinWorkspace(final FlooUrl flooUrl, final String path, final boolean upload) {
        timeouts = new Timeouts(this);
        handler = new FlooHandler(this, flooUrl, upload);
        handler.go();
    }
}
