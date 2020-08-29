package floobits;

import com.intellij.AppTopics;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFilePropertyEvent;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.*;
import com.intellij.util.messages.MessageBusConnection;
import floobits.common.EditorEventHandler;
import floobits.common.Ignore;
import floobits.common.interfaces.IFile;
import floobits.impl.ContextImpl;
import floobits.impl.DocImpl;
import floobits.impl.FactoryImpl;
import floobits.impl.FileImpl;
import floobits.utilities.Flog;
import floobits.utilities.IntelliUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Listener implements BulkFileListener, DocumentListener, SelectionListener, FileDocumentManagerListener, VisibleAreaListener, CaretListener {
    public final AtomicBoolean isListening = new AtomicBoolean(false);
    public final AtomicBoolean isSaving = new AtomicBoolean(false);
    private final ContextImpl context;
    private EditorEventHandler editorManager;
    private MessageBusConnection messageBusConnection;
    private EditorEventMulticaster em;
    private String oldRenamePath;
    private ArrayList<ArrayList<Integer>> ranges = new ArrayList<ArrayList<Integer>>();

    public Listener(ContextImpl context) {
        this.context = context;
    }

    public synchronized void start(final EditorEventHandler editorManager_) {
        editorManager = editorManager_;
        messageBusConnection = ApplicationManager.getApplication().getMessageBus().connect();
        messageBusConnection.subscribe(VirtualFileManager.VFS_CHANGES, this);
        messageBusConnection.subscribe(AppTopics.FILE_DOCUMENT_SYNC, this);
        messageBusConnection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            List<? extends VFileEvent> beforeEvents;
//            before/after could maybe be in different threads?
            @Override
            public void before(@NotNull List<? extends VFileEvent> events) {
                beforeEvents = events;
            }

            @Override
            public void after(@NotNull List<? extends VFileEvent> afterEvents) {
                for (int i = 0; i < beforeEvents.size(); i ++) {
//                    VFileEvent vFileEvent = afterEvents.get(i);
//                    if (vFileEvent instanceof VFileMoveEvent) {
//                        Flog.info("move event %s", vFileEvent);
//                        VirtualFile oldParent = ((VFileMoveEvent) vFileEvent).getOldParent();
//                        VirtualFile newParent = ((VFileMoveEvent) vFileEvent).getNewParent();
//                        renameAllNestedFiles(vFileEvent.getFile(), oldParent.getPath(), newParent.getPath());
//                    }
                    String oldPath = beforeEvents.get(i).getPath();
                    String newPath = afterEvents.get(i).getPath();
                    editorManager.rename(oldPath, newPath);
                }
            }
        });

        em = EditorFactory.getInstance().getEventMulticaster();
        final Disposable disposable = new Disposable() {
            @Override
            public void dispose() {
                Flog.debug("disposing");
            }
        };
        em.addDocumentListener(this, disposable);
        em.addSelectionListener(this, disposable);
        em.addCaretListener(this, disposable);
        em.addVisibleAreaListener(this, disposable);
    }

    public synchronized void shutdown() {
        if (em != null) {
            em.removeSelectionListener(this);
            em.removeDocumentListener(this);
            em.removeCaretListener(this);
            em.removeVisibleAreaListener(this);
            em = null;
        }
        if (messageBusConnection != null) {
            messageBusConnection.disconnect();
            messageBusConnection = null;
        }
    }

    @Override
    public void fileWithNoDocumentChanged(@NotNull VirtualFile file) {
       Flog.debug("%s change but has no document.", file.getPath());
    }

    @Override
    public void beforeDocumentSaving(@NotNull Document document) {
        if (isSaving.get()) {
            return;
        }
        FactoryImpl iFactory = (FactoryImpl) context.iFactory;
        String path = iFactory.getPathForDoc(document);
        if (path == null) {
            return;
        }
        editorManager.save(path);
    }


    @Override
    public void documentChanged(DocumentEvent event) {
        if (!isListening.get()) {
            return;
        }
        Document document = event.getDocument();
        Flog.debug("Document change: %s", document);
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        if (virtualFile == null) {
            Flog.info("No virtual file for document %s", document);
            return;
        }
        editorManager.change(new FileImpl(virtualFile));
    }

    public void caretAdded(CaretEvent caretEvent) {
        // Not in use.
    }

    public void caretRemoved(CaretEvent caretEvent) {
        // Not in use.
    }

    @Override
    public void before(@NotNull List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
            if (event instanceof VFileDeleteEvent) {
                Flog.info("deleting a file %s", event.getPath());
                editorManager.deleteDirectory(IntelliUtils.getAllNestedFilePaths(event.getFile()));
                continue;
            }
            if (event instanceof VFilePropertyChangeEvent) {
                VFilePropertyChangeEvent propertyEvent = (VFilePropertyChangeEvent) event;
                if (!propertyEvent.getPropertyName().equals("name")) {
                    continue;
                }
                oldRenamePath = propertyEvent.getFile().getPath();
            }
        }
    }

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
            IFile virtualFile = new FileImpl(event.getFile());
            if (Ignore.isIgnoreFile(virtualFile) && !context.isIgnored(virtualFile)) {
                context.refreshIgnores();
                break;
            }
        }
        if (!isListening.get()) {
            return;
        }
        for (VFileEvent event : events) {
            Flog.debug(" after event type %s", event.getClass().getSimpleName());
            if (event instanceof VFilePropertyChangeEvent) {
                VFilePropertyChangeEvent propertyEvent = (VFilePropertyChangeEvent) event;
                if (!propertyEvent.getPropertyName().equals("name")) {
                    continue;
                }
                VirtualFile virtualFile = propertyEvent.getFile();
                renameAllNestedFiles(virtualFile,  oldRenamePath, virtualFile.getPath());
                oldRenamePath = null;
                continue;
            }
            if (event instanceof VFileMoveEvent) {
                Flog.info("move event %s", event);
                VirtualFile oldParent = ((VFileMoveEvent) event).getOldParent();
                VirtualFile newParent = ((VFileMoveEvent) event).getNewParent();
                renameAllNestedFiles(event.getFile(), oldParent.getPath(), newParent.getPath());
                continue;
            }

            if (event instanceof VFileCopyEvent) {
                // We get one copy event per file copied for copied directories, which makes this easy.
                Flog.info("Copying a file %s", event);
                VirtualFile newParent = ((VFileCopyEvent) event).getNewParent();
                String newChildName = ((VFileCopyEvent) event).getNewChildName();
                String path = event.getPath();
                VirtualFile[] children = newParent.getChildren();
                VirtualFile copiedFile = null;
                for (VirtualFile child : children) {
                    if (child.getName().equals(newChildName)) {
                        copiedFile = child;
                        break;
                    }
                }
                if (copiedFile == null) {
                    Flog.error("Couldn't find copied virtual file %s", path);
                    continue;
                }
                editorManager.createFile(new FileImpl(copiedFile));
                continue;
            }
            if (event instanceof VFileCreateEvent) {
                Flog.info("creating a file %s", event);
                ArrayList<IFile> createdFiles = IntelliUtils.getAllValidNestedFiles(context, event.getFile());
                for (final IFile createdFile : createdFiles) {
                    editorManager.createFile(createdFile);
                }
                continue;
            }
            if (event instanceof VFileContentChangeEvent) {
                ArrayList<IFile> changedFiles = IntelliUtils.getAllValidNestedFiles(context, event.getFile());
                for (IFile file : changedFiles) {
                    editorManager.change(file);
                }
            }
        }
    }

    private void renameAllNestedFiles(VirtualFile virtualFile, String oldPath, String newPath) {
        ArrayList<IFile> files = IntelliUtils.getAllValidNestedFiles(context, virtualFile);
        for (IFile file: files) {
            String newFilePath = file.getPath();
            String oldFilePath = newFilePath.replace(newPath, oldPath);
            editorManager.rename(oldFilePath, newFilePath);
        }
    }

    @Override
    public void unsavedDocumentsDropped() {
    }

    @Override
    public void beforeFileContentReload(VirtualFile file, @NotNull Document document) {
    }

    @Override
    public void fileContentReloaded(@NotNull VirtualFile file, @NotNull Document document) {
    }

    @Override
    public void fileContentLoaded(@NotNull VirtualFile file, @NotNull Document document) {
    }

    @Override
    public void beforeAllDocumentsSaving() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void beforeDocumentChange(final DocumentEvent event) {
        if (!isListening.get()) {
            return;
        }
        if (!event.getDocument().isWritable()) {
            Flog.log("Document is not writable? %s", event.getDocument());
        }
        final VirtualFile file = FileDocumentManager.getInstance().getFile(event.getDocument());
        if (file == null)
            return;

        Document document = event.getDocument();
        editorManager.beforeChange(new DocImpl(context, document));
    }

    @Override
    public void caretPositionChanged(final CaretEvent event) {
        sendCaretPosition(event.getEditor(), false);
    }

    @Override
    public void visibleAreaChanged(final VisibleAreaEvent event) {
        sendCaretPosition(event.getEditor(), true);
    }

    private void sendCaretPosition(Editor editor, boolean following) {
        FactoryImpl iFactory = (FactoryImpl) context.iFactory;
        Document document = editor.getDocument();
        String path = iFactory.getPathForDoc(document);
        if (path == null) {
            return;
        }

        ArrayList<ArrayList<Integer>> rangesWithCaret = new ArrayList<ArrayList<Integer>>(ranges.size() + 1);
        rangesWithCaret.addAll(ranges);
        Integer offset = editor.getCaretModel().getOffset();
        rangesWithCaret.add(new ArrayList<Integer>(Arrays.asList(offset, offset)));
        editorManager.changeSelection(path, rangesWithCaret, !isListening.get() || following);
    }

    @Override
    public void selectionChanged(final SelectionEvent event) {
        Document document = event.getEditor().getDocument();
        FactoryImpl iFactory = (FactoryImpl) context.iFactory;
        String path = iFactory.getPathForDoc(document);
        if (path == null) {
            return;
        }

        TextRange[] textRanges = event.getNewRanges();
        ranges = new ArrayList<ArrayList<Integer>>();
        for(TextRange r : textRanges) {
            int start = r.getStartOffset();
            int end = r.getEndOffset();
            if (start == end) {
                //This signifies a selection was cleared. We don't want to store that as a range.
                continue;
            }
            ranges.add(new ArrayList<Integer>(Arrays.asList(start, end)));
        }
        editorManager.changeSelection(path, ranges, !isListening.get());
    }

}
