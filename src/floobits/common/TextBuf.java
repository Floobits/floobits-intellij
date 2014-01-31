package floobits.common;

import com.intellij.openapi.editor.*;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import dmp.FlooDmp;
import dmp.FlooPatchPosition;
import dmp.diff_match_patch;
import floobits.FloobitsPlugin;
import floobits.Listener;
import floobits.common.protocol.FlooPatch;
import floobits.handlers.FlooHandler;
import floobits.utilities.Flog;
import floobits.utilities.ThreadSafe;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class TextBuf extends Buf <String> {
    protected static FlooDmp dmp = new FlooDmp();
    protected Timeouts timeouts = Timeouts.create();
    public TextBuf (String path, Integer id, String buf, String md5) {
        super(path, id, buf, md5);
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
        ThreadSafe.write(new Runnable() {
            public void run() {
                VirtualFile virtualFile = getVirtualFile();
                if (virtualFile == null) {
                    virtualFile = createFile();
                    if (virtualFile == null) {
                        Flog.throwAHorribleBlinkingErrorAtTheUser("Unable to write file.");
                        return;
                    }
                }
                Document d = Buf.getDocumentForVirtualFile(virtualFile);
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
                    Flog.throwAHorribleBlinkingErrorAtTheUser(e);
                }
            }
        });
    }

    synchronized public void set(String s, String newMD5) {
        buf = NEW_LINE.matcher(s).replaceAll("\n");
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
        FlooHandler flooHandler = FloobitsPlugin.getHandler();
        if (flooHandler == null) {
            return;
        }
        String previous = buf;
        String before_md5 = md5;
        String after_md5 = DigestUtils.md5Hex(current);
        LinkedList<diff_match_patch.Patch> patches = dmp.patch_make(previous, current);
        String textPatch = dmp.patch_toText(patches);
        set(current, after_md5);
        flooHandler.send_patch(textPatch, before_md5, this);
    }

    public void patch(final FlooPatch res) {
        final TextBuf b = this;
        Flog.info("Got _on_patch");
        ThreadSafe.read(new Runnable() {
            @Override
            public void run() {
                final Document d;
                final FlooHandler flooHandler = FloobitsPlugin.getHandler();
                if (flooHandler == null) {
                    return;
                }
                String oldText = buf;
                VirtualFile virtualFile = b.getVirtualFile();
                if (virtualFile == null) {
                    Flog.warn("VirtualFile is null, no idea what do do. Aborting everything %s", this);
                    buf = null;
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
                        d.setReadOnly(true);
                        return;
                    }
                }
                // XXX: If patchedContents have carriage returns this will be a problem:
                String md5After = DigestUtils.md5Hex(patchedContents);
                if (!md5After.equals(res.md5_after)) {
                    Flog.info("MD5 after mismatch (ours %s remote %s). Sending get_buf soon.", md5After, res.md5_after);
                    final Integer buf_id = b.id;
                    Timeout timeout = new Timeout(2000) {
                        @Override
                        public void run(Void arg) {
                            b.timeout = null;
                            Flog.info("Sending get buf because md5s did not match.");
                            FloobitsPlugin.getHandler().send_get_buf(buf_id);
                        }
                    };
                    timeouts.setTimeout(timeout);
                    b.timeout = timeout;
                    return;
                }
                Flog.log("Patched %s", res.path);
                ThreadSafe.write(new Runnable() {
                    @Override
                    public void run() {
                        if (!ReadonlyStatusHandler.ensureDocumentWritable(flooHandler.project, d)){
                            Flog.info("Document: %s is not writable.", d);
                            return;
                        }

                        final Editor[] editors = EditorFactory.getInstance().getEditors(d, FloobitsPlugin.getHandler().project);
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
                            int end = Math.min(start + flooPatchPosition.end, d.getTextLength());
                            String contents = NEW_LINE.matcher(flooPatchPosition.text).replaceAll("\n");
                            Exception e = null;
                            try {
                                Listener.flooDisable();
                                d.replaceString(start, end, contents);
                            } catch (Exception exception) {
                                e = exception;
                            } finally {
                                Listener.flooEnable();
                            }

                            if (e != null) {
                                Flog.throwAHorribleBlinkingErrorAtTheUser(e);
                                FloobitsPlugin.getHandler().send_get_buf(id);
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
                        for (Map.Entry<ScrollingModel, Integer[]> entry : original.entrySet()) {
                            ScrollingModel model = entry.getKey();
                            Integer[] offsets = entry.getValue();
                            model.scrollHorizontally(offsets[0]);
                            model.scrollVertically(offsets[1]);
                        }
                        b.set(d.getText(), res.md5_after);
                    }
                });
            }
        });
    }
}
