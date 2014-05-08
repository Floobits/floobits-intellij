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
    public final ConcurrentLinkedQueue<QueuedAction> queue = new ConcurrentLinkedQueue<QueuedAction>();
    private final FlooContext context;
    private final FloobitsState state;
    // buffer ids are not removed from readOnlyBufferIds
    public HashSet<Integer> readOnlyBufferIds = new HashSet<Integer>();
    private final Runnable dequeueRunnable = new Runnable() {
        @Override
        public void run() {
            Flog.log("Doing %s work", queue.size());
            while (true) {
                // TODO: set a limit here and continue later
                QueuedAction action = queue.poll();
                if (action == null) {
                    return;
                }
                action.run();
            }
        }
    };

    public void clearReadOnlyState() {
        for (Integer bufferId : readOnlyBufferIds) {
            Buf buf = state.bufs.get(bufferId);
            if (buf == null) {
                continue;
            }
            buf.clearReadOnly();
        }
        readOnlyBufferIds.clear();
    }

    class QueuedAction {
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
            long l1 = System.currentTimeMillis();
            Flog.log("Spent %s in ui thread", l1 -l);
        }
    }

    public void shutdown() {
        clearHighlights();
        queue.clear();
        clearReadOnlyState();
    }

    public EditorManager(FlooContext context, FloobitsState state) {
        this.context = context;
        this.state = state;
    }

    public void queue(Buf buf, RunLater<Buf> runnable) {
        if (buf == null) {
            Flog.log("Buf is null abandoning adding new queue action.");
            return;
        }
        QueuedAction queuedAction = new QueuedAction(buf, runnable);
        queue.add(queuedAction);
        if (queue.size() > 1) {
            return;
        }
        ThreadSafe.write(context, dequeueRunnable);
    }

    void remove_highlight(Integer userId, final Integer bufId, final Document document) {
        HashMap<Integer, LinkedList<RangeHighlighter>> integerRangeHighlighterHashMap = state.highlights.get(userId);
        if (integerRangeHighlighterHashMap == null) {
            return;
        }
        final LinkedList<RangeHighlighter> rangeHighlighters = integerRangeHighlighterHashMap.get(bufId);
        if (rangeHighlighters == null) {
            return;
        }
        if (document != null) {
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
            return;
        }

        final Buf buf = this.state.bufs.get(bufId);
        queue(buf, new RunLater<Buf>() {
            public void run(Buf b) {
                Document document = get_document(bufId);
                if (document == null) {
                    return;
                }
                Editor editor = get_editor_for_document(document);
                if (editor == null) {
                    return;
                }
                MarkupModel markupModel = editor.getMarkupModel();
                for (RangeHighlighter rangeHighlighter : rangeHighlighters) {
                    try {
                        markupModel.removeHighlighter(rangeHighlighter);
                    } catch (AssertionError e) {
                        Flog.info("Assertion error on removeHighlighter");
                    } catch (Exception e) {
                        Flog.info(Utils.stackToString(e));
                    }
                }
                rangeHighlighters.clear();
            }
        });

    }
    public void reset() {
        queue.clear();
    }
    public void clearHighlights() {
        if (state.highlights == null || state.highlights.size() <= 0) {
            return;
        }
        for (Map.Entry<Integer, HashMap<Integer, LinkedList<RangeHighlighter>>> entry : state.highlights.entrySet()) {
            HashMap<Integer, LinkedList<RangeHighlighter>> highlightsForUser = entry.getValue();
            if (highlightsForUser == null || highlightsForUser.size() <= 0) {
                continue;
            }
            Integer user_id = entry.getKey();
            for (Map.Entry<Integer, LinkedList<RangeHighlighter>> integerLinkedListEntry: highlightsForUser.entrySet()) {
                remove_highlight(user_id, integerLinkedListEntry.getKey(), null);
            }
        }
    }
    Document get_document(final Integer id) {
        if (state.bufs == null) {
            return null;
        }
        Buf buf = state.bufs.get(id);
        if (buf == null) {
            Flog.info("Buf %d is not populated yet", id);
            return null;
        }
        if (buf.buf == null) {
            Flog.info("Buf %s is not populated yet", buf.path);
            return null;
        }
        String absPath = context.absPath(buf.path);

        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(absPath);
        if (virtualFile == null || !virtualFile.exists()) {
            Flog.info("no virtual file for %s", buf.path);
            return null;
        }
        Document d = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (d == null) {
            return null;
        }
        return d;
    }

    public Editor get_editor_for_document(Document document) {
        Editor[] editors = EditorFactory.getInstance().getEditors(document, context.project);
        for (Editor editor : editors) {
            Flog.warn("is disposed? %s", editor.isDisposed());
        }
        if (editors.length > 0) {
            return editors[0];
        }
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        if (virtualFile == null) {
            return null;
        }
        return EditorFactory.getInstance().createEditor(document, context.project, virtualFile, true);
    }
}
