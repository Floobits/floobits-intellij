package floobits;

import com.intellij.openapi.components.ApplicationComponent;

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.impl.local.FileWatcher;
import com.intellij.openapi.vfs.impl.local.LocalFileSystemImpl;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.events.*;
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl;
import com.intellij.testFramework.PlatformLangTestCase;
import com.intellij.util.Alarm;
import com.intellij.util.Function;
import com.intellij.util.TimeoutUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Listener implements ApplicationComponent, BulkFileListener {
    private static Logger Log = Logger.getInstance(Listener.class);

    private final MessageBusConnection connection;


    public Listener() {
        connection = ApplicationManager.getApplication().getMessageBus().connect();
    }

    public void initComponent() {
        connection.subscribe(VirtualFileManager.VFS_CHANGES, this);
    }

    public void disposeComponent() {
        connection.disconnect();
    }

    @Override
    public void before(@NotNull List<? extends VFileEvent> events) {
        Log.info("Joining workspace... NOT");
    }
    @Override
    public void after(List<? extends VFileEvent> events) {
        Log.info("Joining workspace... NOT");
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "asdf";  //To change body of implemented methods use File | Settings | File Templates.
    }
}