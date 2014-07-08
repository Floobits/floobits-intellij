package floobits.Virtual;

import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import floobits.FlooContext;
import floobits.Listener;
import floobits.common.VDoc;
import floobits.common.protocol.receive.FlooHighlight;
import floobits.utilities.Colors;
import floobits.utilities.Flog;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by kans on 7/7/14.
 */
public class IntellijDoc extends VDoc{
    private Document document;

    @Override
    public void applyHighlight(String username, FlooContext context, Boolean force, FlooHighlight highlight) {

        final FileEditorManager manager = FileEditorManager.getInstance(context.project);
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        if (virtualFile != null) {
            Boolean summon = false;
            if (highlight.summon != null) {
                summon = highlight.summon;
            }
            if ((highlight.ping || summon) && username != null) {
                context.statusMessage(String.format("%s has summoned you to %s", username, virtualFile.getPath()));
            }
            if (force && virtualFile.isValid()) {
                manager.openFile(virtualFile, true, true);
            }
        }
        editor.remove_highlight(highlight.user_id, buf.path, document);

        int textLength = document.getTextLength();
        if (textLength == 0) {
            return;
        }
        TextAttributes attributes = new TextAttributes();
        JBColor color = Colors.getColorForUser(username);
        attributes.setEffectColor(color);
        attributes.setEffectType(EffectType.SEARCH_MATCH);
        attributes.setBackgroundColor(color);
        attributes.setForegroundColor(Colors.getFGColor());

        boolean first = true;
        Editor[] editors = EditorFactory.getInstance().getEditors(document, context.project);

        for (Editor editor : editors) {
            if (editor.isDisposed()) {
                continue;
            }
            MarkupModel markupModel = editor.getMarkupModel();
            LinkedList<RangeHighlighter> rangeHighlighters = new LinkedList<RangeHighlighter>();
            for (List<Integer> range : ranges) {
                int start = range.get(0);
                int end = range.get(1);
                if (start == end) {
                    end += 1;
                }
                if (end > textLength) {
                    end = textLength;
                }
                if (start >= textLength) {
                    start = textLength - 1;
                }
                RangeHighlighter rangeHighlighter = null;
                try {
                    Listener.flooDisable();
                    rangeHighlighter = markupModel.addRangeHighlighter(start, end, HighlighterLayer.ERROR + 100,
                            attributes, HighlighterTargetArea.EXACT_RANGE);
                } catch (Throwable e) {
                    Flog.warn(e);
                } finally {
                    Listener.flooEnable();
                }
                if (rangeHighlighter == null) {
                    continue;
                }
                rangeHighlighters.add(rangeHighlighter);
                if (force && first) {
                    CaretModel caretModel = editor.getCaretModel();
                    caretModel.moveToOffset(start);
                    LogicalPosition position = caretModel.getLogicalPosition();
                    ScrollingModel scrollingModel = editor.getScrollingModel();
                    scrollingModel.scrollTo(position, ScrollType.MAKE_VISIBLE);
                    first = false;
                }
            }
            HashMap<String, LinkedList<RangeHighlighter>> integerRangeHighlighterHashMap = fleditor.highlights.get(highlight.user_id);

            if (integerRangeHighlighterHashMap == null) {
                integerRangeHighlighterHashMap = new HashMap<String, LinkedList<RangeHighlighter>>();
                fleditor.highlights.put(highlight.user_id, integerRangeHighlighterHashMap);
            }
            integerRangeHighlighterHashMap.put(buf.path, rangeHighlighters);
        }
    }

    @Override
    public void save(FlooContext context) {
        if (!ReadonlyStatusHandler.ensureDocumentWritable(context.project, document)) {
            Flog.info("Document: %s is not writable, can not save.", document);
            return;
        }
        FileDocumentManager.getInstance().saveDocument(document);

    }

    @Override
    public String getText() {
        return document.getText();
    }

    public IntellijDoc(Document document) {
        this.document = document;
    }

    @Override
    public void removeHighlight(FlooContext context, Object obj) {
        final LinkedList<RangeHighlighter> rangeHighlighters = (LinkedList<RangeHighlighter>) obj;
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
}
