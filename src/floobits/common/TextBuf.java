package floobits.common;

import com.intellij.openapi.editor.*;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import dmp.FlooDmp;
import dmp.FlooPatchPosition;
import dmp.diff_match_patch;
import floobits.FlooContext;
import floobits.common.protocol.FlooPatch;
import floobits.handlers.FlooHandler;
import floobits.utilities.Flog;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class TextBuf extends Buf <String> {
    protected static FlooDmp dmp = new FlooDmp();
    public  TextBuf(String path, Integer id, String buf, String md5, FlooContext context) {
        super(path, id, buf, md5, context);
        if (buf != null) {
            this.buf = NEW_LINE.matcher(buf).replaceAll("\n");
        }
        this.encoding = Encoding.UTF8;
    }

    public void read () {
        VirtualFile virtualFile = this.getVirtualFile();
        if (virtualFile == null) {
            Flog.warn("Can't get virtual file to read from disk %s", this);
            return;
        }
        Document d = Buf.getDocumentForVirtualFile(virtualFile);
        if (d == null) {
            Flog.warn("Can't get document to read from disk %s", this);
            return;
        }
        this.buf = d.getText();
        this.md5 = DigestUtils.md5Hex(this.buf);
        }

    public void write() {
        if (!isPopulated()) {
            return;
        }
        VirtualFile virtualFile = getVirtualFile();
        if (virtualFile == null) {
            virtualFile = createFile();
            if (virtualFile == null) {
                Utils.error_message("The Floobits plugin was unable to write to a file.", context.project);
                return;
            }
        }
        Document d = Buf.getDocumentForVirtualFile(virtualFile);
        if (d != null) {
            FlooHandler flooHandler = context.getFlooHandler();
            if (flooHandler == null) {
                return;
            }
            try {
                flooHandler.listener.flooDisable();
                d.setReadOnly(false);
                d.setText(buf);
            } finally {
                flooHandler.listener.flooEnable();
            }
            return;
        }
        Flog.warn("Tried to write to null document: %s", path);
        try {
            virtualFile.setBinaryContent(buf.getBytes());
        } catch (IOException e) {
            Flog.warn(e);
            Utils.error_message("The Floobits plugin was unable to write to a file.", context.project);
        }
    }

    synchronized public void set(String s, String newMD5) {
        buf = s == null ? null : NEW_LINE.matcher(s).replaceAll("\n");
        md5 = newMD5;
    }

    public String serialize() {
        return buf;
    }

    public void send_patch(VirtualFile virtualFile) {
        Document d = Buf.getDocumentForVirtualFile(virtualFile);
        if (d == null) {
            Flog.warn("Can't get document to read from disk for sending patch %s", path);
            return;
        }
        send_patch(d.getText());
    }

    public void send_patch(String current) {
        FlooHandler flooHandler = context.getFlooHandler();
        if (flooHandler == null) {
            return;
        }
        String before_md5;
        String textPatch;
        String after_md5;

        String previous = buf;
        before_md5 = md5;
        after_md5 = DigestUtils.md5Hex(current);
        LinkedList<diff_match_patch.Patch> patches = dmp.patch_make(previous, current);
        textPatch = dmp.patch_toText(patches);

        set(current, after_md5);
        if (before_md5.equals(after_md5)) {
            Flog.log("Not patching %s because no change.", path);
            return;
        }
        flooHandler.send_patch(textPatch, before_md5, this);
    }

    private void setGetBufTimeout() {
        final int buf_id = id;
        cancelTimeout();
        this.timeout = context.setTimeout(2000, new Runnable() {
            @Override
            public void run() {
                Flog.info("Sending get buf after timeout.");
                FlooHandler flooHandler = context.getFlooHandler();
                if (flooHandler == null) {
                    return;
                }
                flooHandler.send_get_buf(buf_id);
            }
        });
    }

    @Override
    public void cleanUp() {
        super.cleanUp();
        Document document = getDocumentForVirtualFile(getVirtualFile());
        if (document != null) {
            document.setReadOnly(false);
        }
    }

    public void patch(final FlooPatch res) {
        final TextBuf b = this;
        Flog.info("Got _on_patch");

        String text;
        String md5FromDoc;
        final Document d;
        final FlooHandler flooHandler = context.getFlooHandler();
        if (flooHandler == null) {
            return;
        }
        String oldText = buf;
        VirtualFile virtualFile = b.getVirtualFile();
        if (virtualFile == null) {
            Flog.warn("VirtualFile is null, no idea what do do. Aborting everything %s", this);
            flooHandler.send_get_buf(id);
            return;
        }
        d = Buf.getDocumentForVirtualFile(virtualFile);
        if (d == null) {
            Flog.warn("Document not found for %s", virtualFile);
            return;
        }
        String viewText;
        if (virtualFile.exists()) {
            viewText = d.getText();
            if (viewText.equals(oldText)) {
                b.forced_patch = false;
            } else if (!b.forced_patch) {
                b.forced_patch = true;
                oldText = viewText;
                b.send_patch(viewText);
                Flog.warn("Sending force patch for %s. this is dangerous!", b.path);
            }
        } else {
            viewText = oldText;
        }

        b.cancelTimeout();

        String md5Before = DigestUtils.md5Hex(viewText);
        if (!md5Before.equals(res.md5_before)) {
            Flog.warn("starting md5s don't match for %s. this is dangerous!", b.path);
        }

        LinkedList patches = (LinkedList) dmp.patch_fromText(res.patch);
        final Object[] results = dmp.patch_apply(patches, oldText);
        final String patchedContents = (String) results[0];
        final boolean[] patchesClean = (boolean[]) results[1];
        final FlooPatchPosition[] positions = (FlooPatchPosition[]) results[2];

        for (boolean clean : patchesClean) {
            if (!clean) {
                Flog.log("Patch not clean for %s. Sending get_buf and setting readonly.", d);
                flooHandler.send_get_buf(res.id);
                return;
            }
        }
        // XXX: If patchedContents have carriage returns this will be a problem:
        String md5After = DigestUtils.md5Hex(patchedContents);
        if (!md5After.equals(res.md5_after)) {
            Flog.info("MD5 after mismatch (ours %s remote %s)", md5After, res.md5_after);
        }

        if (!d.isWritable()) {
            d.setReadOnly(false);
        }
        if (!ReadonlyStatusHandler.ensureDocumentWritable(context.project, d)) {
            Flog.info("Document: %s is not writable.", d);
            return;
        }

        final Editor[] editors = EditorFactory.getInstance().getEditors(d, context.project);
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
            Exception e = null;
            try {
                flooHandler.listener.flooDisable();
                d.replaceString(start, end_ld, contents);
            } catch (Exception exception) {
                e = exception;
            } finally {
                flooHandler.listener.flooEnable();
            }

            if (e != null) {
                Flog.warn(e);
                flooHandler.send_get_buf(id);
                return;
            }

            for (Editor editor : editors) {
                if (editor.isDisposed()) {
                    continue;
                }
                CaretModel caretModel = editor.getCaretModel();
                int offset = caretModel.getOffset();
                if (offset < start) {
                    continue;
                }
                int newOffset = offset + contents.length() - flooPatchPosition.end;
                Flog.log("Moving cursor from %s to %s", offset, newOffset);

                try {
                    caretModel.moveToOffset(newOffset);
                } catch (Exception e1) {
                    Flog.info("Can't move caret: %s", e1);
                }
            }
        }
        text = d.getText();
        md5FromDoc = DigestUtils.md5Hex(text);
        if (!md5FromDoc.equals(res.md5_after)) {
            Flog.info("md5FromDoc mismatch (ours %s remote %s)", md5FromDoc, res.md5_after);
            b.setGetBufTimeout();
        }

        for (Map.Entry<ScrollingModel, Integer[]> entry : original.entrySet()) {
            ScrollingModel model = entry.getKey();
            Integer[] offsets = entry.getValue();
            model.scrollHorizontally(offsets[0]);
            model.scrollVertically(offsets[1]);
        }

        b.set(text, md5FromDoc);
        Flog.log("Patched %s", res.path);

    }
}
