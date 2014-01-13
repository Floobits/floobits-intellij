package floobits;

import java.util.List;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.*;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.psi.PsiTreeChangeListener;
import com.intellij.refactoring.RefactoringFactory;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.editor.Document;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.vfs.VirtualFileManager;

public class Listener implements ApplicationComponent, BulkFileListener, DocumentListener, SelectionListener, FileDocumentManagerListener {


    private final MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect();
    private final EditorEventMulticaster em = EditorFactory.getInstance().getEventMulticaster();


    public Listener() {
        VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileAdapter() {
            public void beforePropertyChange(final VirtualFilePropertyEvent event) {
                if (event.getPropertyName().equals(VirtualFile.PROP_NAME)) {
                    VirtualFile parent = event.getParent();
                    String parentPath = parent.getPath();
                    String newValue = parentPath + "/" + event.getNewValue().toString();
                    String oldValue = parentPath + "/" + event.getOldValue().toString();
                    FlooHandler instance = FlooHandler.getInstance();
                    if (instance != null) {
                        instance.untellij_renamed(oldValue, newValue);
                    }
                }
            }
        });
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

    @Override
    public void beforeDocumentSaving(@NotNull Document document) {
        GetPath.getPath(new GetPath(document) {
            @Override
            public void if_path(String path, FlooHandler flooHandler) {
                flooHandler.untellij_saved(path);
            }
        });
    }

    @Override
    public void documentChanged(DocumentEvent event) {
        Document document = event.getDocument();
        GetPath.getPath(new GetPath(document) {
            @Override
            public void if_path(String path, FlooHandler flooHandler) {
                flooHandler.untellij_changed(path, document.getText());
            }
        });
    }

    @Override
    public void selectionChanged(final SelectionEvent event) {
        Document document = event.getEditor().getDocument();
        GetPath.getPath(new GetPath(document) {
            @Override
            public void if_path(String path, FlooHandler flooHandler) {
                TextRange[] ranges = event.getNewRanges();
                flooHandler.untellij_selection_change(path, ranges);
            }
        });
    }

    @Override
    public void before(@NotNull List<? extends VFileEvent> events) {
        Flog.info("Before");
    }
    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        FlooHandler handler = FlooHandler.getInstance();
        if (handler == null) {
            return;
        }
        for (VFileEvent event : events) {
            Flog.info(" after event type %s", event.getClass().getSimpleName());
            if (event == null) {
                continue;
            }
            if (event instanceof VFileMoveEvent) {
                if (!Utils.isFile(event.getFile())) {
                    Flog.info("File does not exist..");
                    continue;
                }
                VirtualFile oldParent = ((VFileMoveEvent) event).getOldParent();
                VirtualFile newParent = ((VFileMoveEvent) event).getNewParent();
                VirtualFile movedFile = event.getFile();
                String fileName = movedFile.getName();
                FlooHandler.getInstance().untellij_renamed(
                        oldParent.getPath() + "/" + fileName,
                        newParent.getPath() + "/" + fileName);
                Flog.info("move event %s", event);
                continue;
            }
            if (event instanceof VFileDeleteEvent) {
                Flog.info("deleting a file %s", event.getPath());
                VirtualFile file = event.getFile();
                if (file.isDirectory()) {
                    handler.untellij_deleted_directory(Utils.getAllNestedFilePaths(file));
                }
                handler.untellij_deleted(event.getPath());
                continue;
            }
        }
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "Floobits";  //To change body of implemented methods use File | Settings | File Templates.
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