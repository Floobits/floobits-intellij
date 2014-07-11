package floobits.impl;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.common.EditorScheduler;
import floobits.common.interfaces.IDoc;
import floobits.common.interfaces.IFactory;
import floobits.common.interfaces.IFile;
import floobits.utilities.Flog;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class FactoryImpl implements IFactory {

    private final ContextImpl context;
    private final EditorScheduler editor;
    private final LocalFileSystem instance = LocalFileSystem.getInstance();

    public FactoryImpl(ContextImpl context, EditorScheduler editor) {
        this.context = context;
        this.editor = editor;
    }

    private VirtualFile getVirtualFile(String relPath) {
        VirtualFile fileByPath = instance.findFileByPath(context.absPath(relPath));
        if (fileByPath != null && fileByPath.isValid()) {
            return fileByPath;
        }
        return null;
    }

    public IDoc makeVFile(FileImpl vFile) {
        Document document = FileDocumentManager.getInstance().getDocument(vFile.virtualFile);
        if (document == null) {
            return null;
        }
        return new DocImpl(context, document);
    }

    @Override
    public void clearReadOnlyState() {
        for (String path : readOnlyBufferIds) {
            VirtualFile file = getVirtualFile(path);
            if (file == null) {
                continue;
            }
            Document document = FileDocumentManager.getInstance().getDocument(file);
            if (document == null) {
                continue;
            }
            document.setReadOnly(false);
        }
        readOnlyBufferIds.clear();
    }

    @Override
    public void removeHighlightsForUser(int userID) {
        HashMap<String, LinkedList<RangeHighlighter>> integerRangeHighlighterHashMap = DocImpl.highlights.get(userID);
        if (integerRangeHighlighterHashMap == null) {
            return;
        }

        for (Map.Entry<String, LinkedList<RangeHighlighter>> entry : integerRangeHighlighterHashMap.entrySet()) {
            removeHighlight(userID, entry.getKey());
        }
    }

    @Override
    public boolean openFile(File file) {
        VirtualFile floorc = LocalFileSystem.getInstance().findFileByIoFile(file);
        if (floorc == null) {
            return false;
        }
        FileEditorManager.getInstance(context.project).openFile(floorc, true);
        return true;
    }

    @Override
    public void removeHighlight(final Integer userId, final String path) {
        final IFile iFile = findFileByPath(path);
        if (iFile == null) {
            return;
        }
        editor.queue(new Runnable() {
            @Override
            public void run() {
                IDoc iDoc = getDocument(iFile);
                if (iDoc == null) {
                    return;
                }
                iDoc.removeHighlight(userId, path);
            }
        });
    }

    @Override
    public void clearHighlights() {
        HashMap<Integer, HashMap<String, LinkedList<RangeHighlighter>>> highlights = DocImpl.highlights;
        if (highlights.size() <= 0) {
            return;
        }
        for (Map.Entry<Integer, HashMap<String, LinkedList<RangeHighlighter>>> entry : highlights.entrySet()) {
            HashMap<String, LinkedList<RangeHighlighter>> highlightsForUser = entry.getValue();
            if (highlightsForUser == null || highlightsForUser.size() <= 0) {
                continue;
            }
            Integer user_id = entry.getKey();
            for (Map.Entry<String, LinkedList<RangeHighlighter>> integerLinkedListEntry: highlightsForUser.entrySet()) {
                removeHighlight(user_id, integerLinkedListEntry.getKey());
            }
        }
    }

    @Override
    public IFile findFileByPath(String path) {
        VirtualFile fileByPath = instance.findFileByPath(context.absPath(path));
        if (fileByPath != null && fileByPath.isValid()) {
            return new FileImpl(fileByPath);
        }
        return null;
    }

    @Override
    public IFile findFileByIoFile(File file) {
        VirtualFile fileByIoFile = VfsUtil.findFileByIoFile(file, true);
        if (fileByIoFile == null) {
            return null;
        }
        return new FileImpl(fileByIoFile);
    }

    @Override
    public IDoc getDocument(IFile virtualFile) {
        if (virtualFile == null) {
            return null;
        }
        Document document = FileDocumentManager.getInstance().getDocument(((FileImpl) virtualFile).virtualFile);
        if (document == null) {
            return null;
        }
        return new DocImpl(context, document);
    }

    @Override
    public IDoc getDocument(String relPath) {
        IFile fileByPath = findFileByPath(context.absPath(relPath));
        return getDocument(fileByPath);
    }

    @Override
    public IFile createDirectories(String path) {
        VirtualFile directory = null;
        try {
            directory = VfsUtil.createDirectories(path);
        } catch (IOException e) {
            Flog.warn(e);
        }

        if (directory == null) {
            Flog.warn("Failed to create directories %s %s", path);
            return null;
        }
        return new FileImpl(directory);
    }
}
