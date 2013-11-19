package floobits;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.*;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.openapi.editor.EditorFactory;
import org.jetbrains.annotations.NotNull;


import java.util.List;

// see https://android.googlesource.com/platform/tools/idea/+/refs/heads/snapshot-master/platform/editor-ui-api/src/com/intellij/openapi/editor/event/EditorEventMulticaster.java
public class Listener implements ApplicationComponent, BulkFileListener, DocumentListener, SelectionListener{
    private static Logger Log = Logger.getInstance(Listener.class);

    private final MessageBusConnection connection;
    private final EditorEventMulticaster em;


    public Listener() {
        connection = ApplicationManager.getApplication().getMessageBus().connect();
        em = EditorFactory.getInstance().getEventMulticaster();
    }


    public void initComponent() {
        connection.subscribe(VirtualFileManager.VFS_CHANGES, this);
        em.addDocumentListener(this);
        em.addSelectionListener(this);
    }

    @Override
    public void beforeDocumentChange(DocumentEvent event) {
        Log.info(String.format("beforeDocumentChange, %s", event));
    }

    @Override
    public void documentChanged(DocumentEvent event) {
        Log.info(String.format("documentChanged, %s", event));
    }

    @Override
    public void selectionChanged(SelectionEvent event) {
        Log.info(String.format("selectionChanged, %s", event));
    }

    public void disposeComponent() {
        connection.disconnect();
    }

    @Override
    public void before(@NotNull List<? extends VFileEvent> events) {
        Log.info("before");
    }
    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        Log.info("after");
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "asdf";  //To change body of implemented methods use File | Settings | File Templates.
    }
}