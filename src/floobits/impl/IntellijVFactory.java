package floobits.impl;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.FlooContext;
import floobits.common.interfaces.VDoc;
import floobits.common.interfaces.VFactory;
import floobits.common.interfaces.VFile;
import floobits.utilities.Flog;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


public class IntellijVFactory implements VFactory {

    private FlooContext context;

    public IntellijVFactory(FlooContext context) {
        this.context = context;
    }

    @Override
    public void clearReadOnlyState() {
        for (String path : readOnlyBufferIds) {
            VDoc document = context.vFactory.getDocument(path);
            if (document != null) {
                document.setReadOnly(false);
            }
        }
        readOnlyBufferIds.clear();
    }

    @Override
    public void removeHighlightsForUser(int userID) {
        HashMap<String, LinkedList<RangeHighlighter>> integerRangeHighlighterHashMap = IntellijDoc.highlights.get(userID);
        if (integerRangeHighlighterHashMap == null) {
            return;
        }

        for (Map.Entry<String, LinkedList<RangeHighlighter>> entry : integerRangeHighlighterHashMap.entrySet()) {
            removeHighlight(userID, entry.getKey());
        }
    }

    @Override
    public boolean openFile(FlooContext context, File file) {
        VirtualFile floorc = LocalFileSystem.getInstance().findFileByIoFile(file);
        if (floorc == null) {
            return false;
        }
        FileEditorManager.getInstance(context.project).openFile(floorc, true);
        return true;
    }

    @Override
    void removeHighlight(Integer userId, final String path) {
        VDoc vDoc = context.vFactory.getDocument(path);
        removeHighlight(userId, path, vDoc);
    }

    @Override
    public void removeHighlight(Integer userId, final String path, final VDoc document) {
        HashMap<String, LinkedList<RangeHighlighter>> integerRangeHighlighterHashMap = IntellijDoc.highlights.get(userId);
        if (document == null) {
            return;
        }
        if (integerRangeHighlighterHashMap == null) {
            return;
        }
        final LinkedList<RangeHighlighter> rangeHighlighters = integerRangeHighlighterHashMap.get(path);
        if (rangeHighlighters == null) {
            return;
        }
        queue(new Runnable() {
            @Override
            public void run() {
                document.removeHighlight(rangeHighlighters);
            }
        });
    }

    @Override
    public void clearHighlights() {
        HashMap<Integer, HashMap<String, LinkedList<RangeHighlighter>>> highlights = IntellijDoc.highlights;
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
    public VFile findFileByPath(String path) {
        LocalFileSystem instance = LocalFileSystem.getInstance();
        VirtualFile fileByPath = instance.findFileByPath(context.absPath(path));
        if (fileByPath != null && fileByPath.isValid()) {
            return new IntellijFile(fileByPath);
        }
        return null;
    }

    @Override
    public VFile findFileByIoFile(File file) {
        VirtualFile fileByIoFile = VfsUtil.findFileByIoFile(file, true);
        if (fileByIoFile == null) {
            return null;
        }
        return new IntellijFile(fileByIoFile);
    }

    @Override
    public VDoc getDocument(String path) {
        String absPath = context.absPath(path);

        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(absPath);
        if (virtualFile == null) {
            Flog.info("no virtual file for %s", path);
            return null;
        }
        Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (document == null) {
            return null;
        }
        return new IntellijDoc(context, document);
    }

    @Override
    public VFile createDirectories(String path) {
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
        return new IntellijFile(directory);
    }
}
