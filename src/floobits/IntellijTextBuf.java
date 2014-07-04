package floobits;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ScrollingModel;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.common.OutboundRequestHandler;
import floobits.common.TextBuf;
import floobits.common.dmp.FlooPatchPosition;
import floobits.utilities.Flog;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class IntellijTextBuf extends TextBuf<VirtualFile> {
    public IntellijTextBuf(String path, Integer id, String buf, String md5, FlooContext context, OutboundRequestHandler outbound) {
        super(path, id, buf, md5, context, outbound);
    }

    private Document getDocument() {
        VirtualFile virtualFile = BufHelper.getVirtualFile(context, path);
        if (virtualFile == null) {
            Flog.warn("Can't get virtual file to read from disk %s", this);
            return null;
        }
        Document d = BufHelper.getDocumentForVirtualFile(virtualFile);
        if (d == null) {
            Flog.warn("Can't get document to read from disk %s", this);
            return null;
        }
        return d;
    }

    @Override
    public VirtualFile createFile() {
        return BufHelper.createFile(context, path);
    }

    @Override
    protected String getText(VirtualFile f) {
        if (f == null) {
            Flog.warn("Can't get virtual file to read from disk %s", this);
            return null;
        }
        Document d = BufHelper.getDocumentForVirtualFile(f);
        if (d == null) {
            Flog.warn("Can't get document to read from disk %s", this);
            return null;
        }
        return d.getText();
    }

    @Override
    public String getText() {
        Document d = getDocument();
        return d != null ? d.getText() : null ;
    }

    @Override
    public void updateView() {
        VirtualFile virtualFile = BufHelper.getVirtualFile(context, path);
        if (virtualFile == null) {
            virtualFile = createFile();
            if (virtualFile == null) {
                context.errorMessage("The Floobits plugin was unable to write to a file.");
                return;
            }
        }
        Document d = BufHelper.getDocumentForVirtualFile(virtualFile);
        if (d != null) {
            try {
                Listener.flooDisable();
                d.setReadOnly(false);
                d.setText(buf);
            } finally {
                Listener.flooEnable();
            }
            return;
        }
        Flog.warn("Tried to write to null document: %s", path);
        try {
            virtualFile.setBinaryContent(buf.getBytes());
        } catch (IOException e) {
            Flog.warn(e);
            context.errorMessage("The Floobits plugin was unable to write to a file.");
        }
    }

    @Override
    protected Object[] applyPatch(FlooPatchPosition[] positions) {
        Document d = getDocument();
        if (d == null) {
            Flog.warn("Document not found for %s", path);
            getBuf();
            return null;
        }
        if (!d.isWritable()) {
            d.setReadOnly(false);
        }
        if (!ReadonlyStatusHandler.ensureDocumentWritable(context.project, d)) {
            Flog.info("Document: %s is not writable.", d);
            return null;
        }

        final com.intellij.openapi.editor.Editor[] editors = EditorFactory.getInstance().getEditors(d, context.project);
        final HashMap<ScrollingModel, Integer[]> original = new HashMap<ScrollingModel, Integer[]>();
        for (Editor editor : editors) {
            if (editor.isDisposed()) {
                continue;
            }
            ScrollingModel scrollingModel = editor.getScrollingModel();
            original.put(scrollingModel, new Integer[]{scrollingModel.getHorizontalScrollOffset(), scrollingModel.getVerticalScrollOffset()});
        }
        for (FlooPatchPosition flooPatchPosition : positions) {
            int start = Math.max(0, flooPatchPosition.start);
            int end_ld = Math.max(start + flooPatchPosition.end, start);
            end_ld = Math.min(end_ld, d.getTextLength());
            String contents = NEW_LINE.matcher(flooPatchPosition.text).replaceAll("\n");
            Throwable e = null;
            try {
                Listener.flooDisable();
                d.replaceString(start, end_ld, contents);
            } catch (Throwable exception) {
                e = exception;
            } finally {
                Listener.flooEnable();
            }

            if (e != null) {
                Flog.warn(e);
                getBuf();
                return null;
            }
        }
        return new Object[]{d.getText(), original};
    }

    @Override
    protected void updateScroll(Object context) {
        final HashMap<ScrollingModel, Integer[]> original = (HashMap<ScrollingModel, Integer[]>) context;
        for (Map.Entry<ScrollingModel, Integer[]> entry : original.entrySet()) {
            ScrollingModel model = entry.getKey();
            Integer[] offsets = entry.getValue();
            model.scrollHorizontally(offsets[0]);
            model.scrollVertically(offsets[1]);
        }
    }

    @Override
    protected String getViewText() {
        String oldText = buf;
        String viewText;
        VirtualFile virtualFile = BufHelper.getVirtualFile(context, path);
        if (virtualFile == null) {
            Flog.warn("VirtualFile is null, no idea what do do. Aborting everything %s", this);
            getBuf();
            return null;
        }
        Document d = BufHelper.getDocumentForVirtualFile(virtualFile);
        if (d == null) {
            Flog.warn("Document not found for %s", virtualFile);
            getBuf();
            return null;
        }
        if (virtualFile.exists()) {
            viewText = getText();
            if (viewText.equals(oldText)) {
                forced_patch = false;
            } else if (!forced_patch) {
                forced_patch = true;
                send_patch(viewText);
                Flog.warn("Sending force patch for %s. this is dangerous!", path);
            }
        } else {
            viewText = oldText;
        }
        return viewText;
    }

}
