package floobits.common;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.FlooContext;
import floobits.utilities.Flog;
import floobits.utilities.ThreadSafe;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EditorManager {
    public final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<Runnable>();
    private final FlooContext context;
    // buffer ids are not removed from readOnlyBufferIds
    public HashMap<Integer, HashMap<String, LinkedList<RangeHighlighter>>> highlights = new HashMap<Integer, HashMap<String, LinkedList<RangeHighlighter>>>();
    public HashSet<String> readOnlyBufferIds = new HashSet<String>();
    private final Runnable dequeueRunnable = new Runnable() {
        @Override
        public void run() {
            if (queue.size() > 5) {
                Flog.log("Doing %s work", queue.size());
            }
            while (true) {
                // TODO: set a limit here and continue later
                Runnable action = queue.poll();
                if (action == null) {
                    return;
                }
                action.run();
            }
        }
    };

    class QueuedAction implements Runnable {
        public final Buf buf;
        public RunLater<Buf> runnable;

        QueuedAction(Buf buf, RunLater<Buf> runnable) {
            this.runnable = runnable;
            this.buf = buf;
        }
        public void run() {
            long l = System.currentTimeMillis();
            synchronized (buf) {
                runnable.run(buf);
            }
            long l1 = System.currentTimeMillis() - l;
            if (l1 > 200) {
                Flog.log("Spent %s in ui thread", l1);
            }
        }
    }

    public EditorManager(FlooContext context) {
        this.context = context;
    }

    public void clearReadOnlyState() {
        for (String path : readOnlyBufferIds) {
            Document document = get_document(path);
            if (document != null) {
                document.setReadOnly(false);
            }
        }
        readOnlyBufferIds.clear();
    }

    public void shutdown() {
        reset();
        clearReadOnlyState();
    }

    public void queue(Buf buf, RunLater<Buf> runnable) {
        if (buf == null) {
            Flog.log("Buf is null abandoning adding new queue action.");
            return;
        }
        QueuedAction queuedAction = new QueuedAction(buf, runnable);
        queue(queuedAction);
    }

    protected void queue(Runnable runnable) {
        queue.add(runnable);
        if (queue.size() > 1) {
            return;
        }
        ThreadSafe.write(context, dequeueRunnable);
    }

    protected Document get_document(String path) {
        String absPath = context.absPath(path);

        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(absPath);
        if (virtualFile == null) {
            Flog.info("no virtual file for %s", path);
            return null;
        }
        return FileDocumentManager.getInstance().getDocument(virtualFile);
    }

    void remove_highlight(Integer userId, final String path) {
        remove_highlight(userId, path, get_document(path));
    }

    void remove_highlight(Integer userId, final String path, final Document document) {
        HashMap<String, LinkedList<RangeHighlighter>> integerRangeHighlighterHashMap = highlights.get(userId);
        if (integerRangeHighlighterHashMap == null) {
            return;
        }
        final LinkedList<RangeHighlighter> rangeHighlighters = integerRangeHighlighterHashMap.get(path);
        if (rangeHighlighters == null) {
            return;
        }
        if (document == null) {
            return;
        }
        queue(new Runnable() {
            @Override
            public void run() {
                Editor[] editors = EditorFactory.getInstance().getEditors(document, context.project);
                for (Editor editor : editors) {
                    if (editor.isDisposed()) {
                        continue;
                    }
                    MarkupModel markupModel = editor.getMarkupModel();
                    RangeHighlighter[] highlights = markupModel.getAllHighlighters();

                    for (RangeHighlighter rangeHighlighter: rangeHighlighters) {
                        for (RangeHighlighter markupHighlighter : highlights) {
                            if (rangeHighlighter == markupHighlighter) {
                                markupModel.removeHighlighter(rangeHighlighter);
                            }
                        }
                    }
                }
                rangeHighlighters.clear();
            }
        });
    }

    public void reset() {
        queue.clear();
        clearHighlights();
    }

    public void clearHighlights() {
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
                remove_highlight(user_id, integerLinkedListEntry.getKey());
            }
        }
    }
}
