package floobits;

import java.util.List;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.*;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.editor.Document;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.vfs.VirtualFileManager;

abstract class GetPath  {
    public Document document;
    abstract public void if_path(String path, FlooHandler flooHandler);
    public GetPath (Document document) {
        this.document = document;
    }
}

// see https://android.googlesource.com/platform/tools/idea/+/refs/heads/snapshot-master/platform/editor-ui-api/src/com/intellij/openapi/editor/event/EditorEventMulticaster.java
public class Listener implements ApplicationComponent, BulkFileListener, DocumentListener, SelectionListener, FileDocumentManagerListener{


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

    public void disposeComponent() {
        connection.disconnect();
        em.removeSelectionListener(this);
        em.removeDocumentListener(this);
    }

    protected void getPath(GetPath getPath) {
        if (getPath.document == null) {
            return;
        }
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(getPath.document);
        String path;
        try {
            path = virtualFile.getCanonicalPath();
        } catch (NullPointerException e) {
            return;
        }
        FlooHandler flooHandler = FlooHandler.getInstance();
        if (flooHandler == null) {
            return;
        };
        getPath.if_path(path, flooHandler);
    }

    @Override
    public void beforeDocumentSaving(@NotNull Document document) {
        this.getPath(new GetPath(document) {
            @Override
            public void if_path(String path, FlooHandler flooHandler) {
                flooHandler.untellij_saved(path);
            }
        });
    }

    @Override
    public void documentChanged(DocumentEvent event) {
        Document document = event.getDocument();
        this.getPath(new GetPath(document) {
            @Override
            public void if_path(String path, FlooHandler flooHandler) {
                flooHandler.untellij_changed(path, document.getText());
            }
        });
    }

    @Override
    public void selectionChanged(final SelectionEvent event) {
        Document document = event.getEditor().getDocument();
        this.getPath(new GetPath(document) {
            @Override
            public void if_path(String path, FlooHandler flooHandler) {
                TextRange[] ranges = event.getNewRanges();
                flooHandler.untellij_selection_change(path, ranges);
            }
        });
    }

    @Override
    public void before(@NotNull List<? extends VFileEvent> events) {
//        Flog.info("before");
    }
    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
//        Flog.info("after");
    }

    @Override
    public String getComponentName() {
        return "asdf";  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void unsavedDocumentsDropped() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void beforeFileContentReload(VirtualFile file, @NotNull Document document) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void fileWithNoDocumentChanged(@NotNull VirtualFile file) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void fileContentReloaded(VirtualFile file, @NotNull Document document) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void fileContentLoaded(@NotNull VirtualFile file, @NotNull Document document) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void beforeAllDocumentsSaving() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
    @Override
    public void beforeDocumentChange(DocumentEvent event) {
//        Flog.info(String.format("beforeDocumentChange, %s", event));
    }
}