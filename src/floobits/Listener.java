package floobits;

import com.intellij.AppTopics;
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
import floobits.common.Buf;
import floobits.common.Ignore;
import floobits.common.Utils;
import floobits.handlers.FlooHandler;
import floobits.utilities.Flog;
import floobits.utilities.GetPath;
import floobits.utilities.ThreadSafe;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Listener implements BulkFileListener, DocumentListener, SelectionListener, FileDocumentManagerListener, CaretListener {
    private static AtomicBoolean isListening = new AtomicBoolean(true);
    private final FlooContext context;
    private final FlooHandler flooHandler;

    public synchronized void flooEnable() {
        isListening.set(true);
    }
    public synchronized void flooDisable() {
        isListening.set(false);
    }
    private final MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect();
    private final EditorEventMulticaster em = EditorFactory.getInstance().getEventMulticaster();


    public Listener(FlooHandler flooHandler) {
        this.context = flooHandler.context;
        this.flooHandler = flooHandler;
    }

    public void start() {
        connection.subscribe(VirtualFileManager.VFS_CHANGES, this);
        connection.subscribe(AppTopics.FILE_DOCUMENT_SYNC, this);
        em.addDocumentListener(this);
        em.addSelectionListener(this);
        em.addCaretListener(this);


        VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileAdapter() {
            public void beforePropertyChange(final VirtualFilePropertyEvent event) {
                if (!event.getPropertyName().equals(VirtualFile.PROP_NAME)) {
                    return;
                }
                VirtualFile parent = event.getParent();
                if (parent == null) {
                    return;
                }
                String parentPath = parent.getPath();
                String newValue = parentPath + "/" + event.getNewValue().toString();
                String oldValue = parentPath + "/" + event.getOldValue().toString();
                FlooHandler handler = context.getFlooHandler();
                if (handler != null)
                    handler.untellij_renamed(oldValue, newValue);
            }
        }
        );
    }

    public void stop() {
        connection.disconnect();
        em.removeSelectionListener(this);
        em.removeDocumentListener(this);
        em.removeCaretListener(this);
    }

    @Override
    public void fileWithNoDocumentChanged(@NotNull VirtualFile file) {
       Flog.log("%s changed but has no document.", file.getPath());
    }

    @Override
    public void beforeDocumentSaving(@NotNull Document document) {
        GetPath.getPath(context, new GetPath(document) {
            @Override
            public void if_path(String path, FlooHandler flooHandler) {
                flooHandler.untellij_saved(path);
            }
        });
    }


    @Override
    public void documentChanged(DocumentEvent event) {
        Flog.info("Document changed.");
        if (!isListening.get()) {
            return;
        }
        Document document = event.getDocument();
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        if (virtualFile == null) {
            Flog.info("No virtual file for document %s", document);
            return;
        }
        flooHandler.untellij_changed(virtualFile);
    }

    @Override
    public void caretPositionChanged(final CaretEvent event) {
        if (!isListening.get()) {
            return;
        }

        Document document = event.getEditor().getDocument();
        final Editor[] editors = EditorFactory.getInstance().getEditors(document);
        GetPath.getPath(context, new GetPath(document) {
            @Override
            public void if_path(String path, FlooHandler flooHandler) {
                if (editors.length <= 0) {
                    return;
                }
                Editor editor = editors[0];
                ArrayList<ArrayList<Integer>> range = new ArrayList<ArrayList<Integer>>();
                Integer offset = editor.getCaretModel().getOffset();
                range.add(new ArrayList<Integer>(Arrays.asList(offset, offset)));
                flooHandler.untellij_selection_change(path, range);
            }
        });
    }

    @Override
    public void selectionChanged(final SelectionEvent event) {
        if (!isListening.get()) {
            return;
        }
        Document document = event.getEditor().getDocument();
        GetPath.getPath(context, new GetPath(document) {
            @Override
            public void if_path(String path, FlooHandler flooHandler) {
                TextRange[] textRanges = event.getNewRanges();
                ArrayList<ArrayList<Integer>> ranges = new ArrayList<ArrayList<Integer>>();
                for(TextRange r : textRanges) {
                    ranges.add(new ArrayList<Integer>(Arrays.asList(r.getStartOffset(), r.getEndOffset())));
                }
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
        for (VFileEvent event : events) {
            VirtualFile virtualFile = event.getFile();
            if (Ignore.isIgnoreFile(virtualFile) && !context.isIgnored(virtualFile)) {
                context.refreshIgnores();
                break;
            }
        }

        if (!isListening.get()) {
            return;
        }
        for (VFileEvent event : events) {
            Flog.info(" after event type %s", event.getClass().getSimpleName());
            if (event == null) {
                continue;
            }
            if (event instanceof VFileMoveEvent) {
                Flog.info("move event %s", event);
                VirtualFile oldParent = ((VFileMoveEvent) event).getOldParent();
                VirtualFile newParent = ((VFileMoveEvent) event).getNewParent();
                String oldPath = oldParent.getPath();
                String newPath = newParent.getPath();
                VirtualFile virtualFile = event.getFile();
                ArrayList<VirtualFile> files;
                files = Utils.getAllNestedFiles(context, virtualFile);
                for (VirtualFile file: files) {
                    String newFilePath = file.getPath();
                    String oldFilePath = newFilePath.replace(newPath, oldPath);
                    flooHandler.untellij_renamed(oldFilePath, newFilePath);
                }
                continue;
            }
            if (event instanceof VFileDeleteEvent) {
                Flog.info("deleting a file %s", event.getPath());
                flooHandler.untellij_deleted_directory(Utils.getAllNestedFilePaths(context, event.getFile()));
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
                    }
                }
                if (copiedFile == null) {
                    Flog.warn("Couldn't find copied virtual file %s", path);
                    continue;
                }
                Utils.createFile(context, copiedFile);
                continue;
            }
            if (event instanceof VFileCreateEvent) {
                Flog.info("creating a file %s", event);
                ArrayList<VirtualFile> createdFiles;
                createdFiles = Utils.getAllNestedFiles(context, event.getFile());
                for (final VirtualFile createdFile : createdFiles) {
                    Utils.createFile(context, createdFile);
                }
                continue;
            }
            if (event instanceof VFileContentChangeEvent) {
                ArrayList<VirtualFile> changedFiles;
                changedFiles = Utils.getAllNestedFiles(context, event.getFile());
                for (VirtualFile file : changedFiles) {
                    flooHandler.untellij_changed(file);
                }
            }
        }
    }
    @Override
    public void unsavedDocumentsDropped() {
    }

    @Override
    public void beforeFileContentReload(VirtualFile file, @NotNull Document document) {
    }

    @Override
    public void fileContentReloaded(VirtualFile file, @NotNull Document document) {
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

        final Buf bufByPath = flooHandler.get_buf_by_path(file.getPath());
        if (bufByPath == null) {
            return;
        }
        String msg;
        if (flooHandler.readOnly) {
            msg = "This document is readonly because you don't have edit permission in the workspace.";
        } else if (!bufByPath.isPopulated()) {
            msg = "This document is temporarily readonly while we fetch a fresh copy.";
        } else {
            return;
        }
        context.status_message(msg);
        final Document document = event.getDocument();
        document.setReadOnly(true);
        flooHandler.readOnlyBufferIds.add(bufByPath.id);
        final String text = document.getText();
        context.setTimeout(0, new Runnable() {
            @Override
            public void run() {
                ThreadSafe.write(context, new Runnable() {
                    @Override
                    public void run() {
                        if (!flooHandler.readOnly && bufByPath.isPopulated()) {
                            return;
                        }
                        Document d = FileDocumentManager.getInstance().getDocument(file);
                        if (d == null) {
                            return;
                        }
                        try{
                            isListening.set(false);
                            d.setReadOnly(false);
                            d.setText(text);
                            d.setReadOnly(true);
                        } finally {
                            isListening.set(true);
                        }
                    }
                });
            }
        });
    }
}