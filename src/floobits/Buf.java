package floobits;

import java.awt.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Pattern;

import com.intellij.openapi.editor.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import dmp.FlooDmp;
import dmp.FlooPatchPosition;
import dmp.diff_match_patch;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;


enum Encoding {
    BASE64("base64"), UTF8("utf8");
    private final String enc;

    Encoding (String enc) {
        this.enc = enc;
    }

    @Override
    public String toString () {
        return this.enc;
    }

    public static Encoding from (String str) {
        for(Encoding v : Encoding.values()) {
            if(v.toString().equals(str)) {
                return v;
            }
        }
        return null;
    }
}

abstract class Buf <T> {
    static Pattern NEW_LINE = Pattern.compile("\\r\\n?", Pattern.DOTALL);
    public String path;
    public File f;
    public Integer id;
    public String md5;
    public T buf;
    public Encoding encoding;
    public Timeout timeout;
    public boolean forced_patch = false;

    public Buf (String path, Integer id, T buf, String md5) {
        this.id = id;
        this.path = new String(path);
        this.buf = buf;
        this.md5 = new String(md5);
    }

    public String toAbsolutePath () {
        return FilenameUtils.concat(Shared.colabDir, this.path);
    }

    public void cancelTimeout () {
        if (timeout != null) {
            timeout.cancel();
            timeout = null;
        }
    }

    public VirtualFile getVirtualFile() {
        return LocalFileSystem.getInstance().findFileByPath(Utils.absPath(this.path));
    }

    public Boolean isPopulated() {
        return this.id != null && this.buf != null;
    }

    public String toString() {
        return String.format("id: %s path: %s", id, path);
    }

    public VirtualFile createFile() {
        File file = new File(Utils.absPath(path));
        String name = file.getName();
        String parentPath = file.getParent();
        try {
            VfsUtil.createDirectories(parentPath);
        } catch (IOException e) {
            Flog.throwAHorribleBlinkingErrorAtTheUser("createFile error %s", e);
            return null;
        }
        VirtualFile parent = LocalFileSystem.getInstance().findFileByPath(parentPath);
        VirtualFile newFile;
        try {
            newFile = parent.findOrCreateChildData(FlooHandler.getInstance(), name);
        } catch (IOException e) {
            Flog.throwAHorribleBlinkingErrorAtTheUser("Create file error %s", e);
            return null;
        }
        return newFile;
    }

    abstract public void read ();
    abstract public void write();
    abstract public void set (String s, String md5);
    abstract public void patch (FlooPatch res);
    abstract public void send_patch (VirtualFile virtualFile);
    abstract public String serialize();

    static Document getDocumentForVirtualFile(VirtualFile virtualFile) {
        if (virtualFile == null) {
            return null;
        }
        return FileDocumentManager.getInstance().getDocument(virtualFile);
    }
    public static BinaryBuf createBuf (String path, Integer id, byte[] buf, String md5) {
        return new BinaryBuf(path, id, buf, md5);
    }
    public static TextBuf createBuf (String path, Integer id, String buf, String md5) {
        return new TextBuf(path, id, buf, md5);
    }
    public static Buf createBuf (String path, Integer id, Encoding enc, String md5) {
        if (enc == Encoding.BASE64) {
            return new BinaryBuf(path, id, null, md5);
        }
        return new TextBuf(path, id, null, md5);
    }
    public static Buf createBuf (VirtualFile virtualFile) {
        try {
            byte[] originalBytes = virtualFile.contentsToByteArray();
            String encodedContents = new String(originalBytes, "UTF-8");
            byte[] decodedContents = encodedContents.getBytes();
            String filePath = Utils.toProjectRelPath(virtualFile.getPath());
            if (Arrays.equals(decodedContents, originalBytes)) {
                String md5 = DigestUtils.md5Hex(encodedContents);
                return new TextBuf(filePath, null, encodedContents, md5);
            } else {
                String md5 = DigestUtils.md5Hex(originalBytes);
                return new BinaryBuf(filePath, null, originalBytes, md5);
            }
        } catch (IOException e) {
            Flog.warn("Error getting virtual file contents in createBuf %s", virtualFile);
        }
        return null;
    }
}

class BinaryBuf extends Buf <byte[]> {

    public BinaryBuf (String path, Integer id, byte[] buf, String md5) {
        super(path, id, buf, md5);
        this.encoding = Encoding.BASE64;
    }

    public void read () {
        VirtualFile virtualFile = this.getVirtualFile();
        if (virtualFile == null) {
            Flog.warn("Couldn't get virtual file in readFromDisk %s", this);
            return;
        }
        try {
            this.buf = virtualFile.contentsToByteArray();
        } catch (IOException e) {
            Flog.warn("Could not get byte array contents for file %s", this);
            return;
        }
        this.md5 = DigestUtils.md5Hex(this.buf);
    }

    public void write() {
        ThreadSafe.write(new Runnable() {
            @Override
            public void run() {
                VirtualFile virtualFile = getVirtualFile();
                if (virtualFile == null) {
                    virtualFile = createFile();
                    if (virtualFile == null) {
                        Flog.throwAHorribleBlinkingErrorAtTheUser("Unable to write file.");
                        return;
                    }
                }
                try {
                    virtualFile.setBinaryContent(buf);
                } catch (IOException e) {
                    Flog.warn("Writing binary content to disk failed. %s", path);
                }
            }
        });
    }

    public void set (String s, String md5) {
        this.buf = Base64.decodeBase64(s.getBytes(Charset.forName("UTF-8")));
        this.md5 = md5;
    }

    public void set (byte[] s, String md5) {
        this.buf = s;
        this.md5 = md5;
    }

    public String serialize() {
        return Base64.encodeBase64String(buf);
    }

    public void patch(FlooPatch res) {
        FlooHandler flooHandler = FlooHandler.getInstance();
        if (flooHandler == null) {
            return;
        }
        flooHandler.send_get_buf(this.id);
        this.buf = null;
        this.md5 = null;
    }

    public void send_patch(VirtualFile virtualFile) {
        FlooHandler flooHandler = FlooHandler.getInstance();
        if (flooHandler == null) {
            return;
        }
        byte[] contents;
        try {
            contents = virtualFile.contentsToByteArray();
        } catch (IOException e) {
            Flog.warn("Couldn't read contents of binary file. %s", virtualFile);
            return;
        }
        String after_md5 = DigestUtils.md5Hex(contents);
        if (md5.equals(after_md5)) {
            Flog.debug("Binary file changed event but no change in md5 %s", virtualFile);
            return;
        }
        set(contents, after_md5);
        flooHandler.send_set_buf(this);
    };
}

class TextBuf extends Buf <String> {
    protected static FlooDmp dmp = new FlooDmp();
    protected Timeouts timeouts = Timeouts.create();
    public TextBuf (String path, Integer id, String buf, String md5) {
        super(path, id, buf, md5);
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
                if (d == null) {
                    Flog.warn("Tried to write to null document: %s", path);
                    return;
                }
                d.setText(buf);
            }
        });
    }

    public void set (String s, String md5) {
        String contents = NEW_LINE.matcher(s).replaceAll("\n");
        // XXX: This should be handled by workspace.
        if (!contents.equals(s)) {
            Flog.warn("Contents have \\r! Replacing and calling set_buf %s", path);
            this.buf = contents;
            this.md5 = DigestUtils.md5Hex(contents);
            FlooHandler flooHandler = FlooHandler.getInstance();
            if (flooHandler != null) {
                flooHandler.send_set_buf(this);
            }
            return;
        }
        this.buf = s;
        this.md5 = md5;
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
        FlooHandler flooHandler = FlooHandler.getInstance();
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

                String oldText = buf;
                VirtualFile virtualFile = b.getVirtualFile();
                if (virtualFile == null) {
                    Flog.warn("VirtualFile is null, no idea what do do. Aborting everything %s", this);
                    FlooHandler flooHandler = FlooHandler.getInstance();
                    buf = null;
                    if (flooHandler != null) {
                        flooHandler.send_get_buf(id);
                    }
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
                        Flog.info("Patch not clean. Sending get_buf.");
                        FlooHandler.getInstance().send_get_buf(res.id);
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
                        void run(Object... objects) {
                            b.timeout = null;
                            Flog.info("Sending get buf because md5s did not match.");
                            FlooHandler.getInstance().send_get_buf(buf_id);
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
                        final Editor[] editors = EditorFactory.getInstance().getEditors(d, FlooHandler.getInstance().project);
                        final HashMap<ScrollingModel, Integer[]> original = new HashMap<ScrollingModel, Integer[]>();
                        for (Editor editor : editors) {
                            if (editor.isDisposed()) {
                                continue;
                            }
                            ScrollingModel scrollingModel = editor.getScrollingModel();
                            original.put(scrollingModel, new Integer[]{scrollingModel.getHorizontalScrollOffset(), scrollingModel.getVerticalScrollOffset()});
                        }
                        for (FlooPatchPosition flooPatchPosition : positions) {
                            int end = Math.min(flooPatchPosition.start + flooPatchPosition.end, d.getTextLength());
                            String contents = NEW_LINE.matcher(flooPatchPosition.text).replaceAll("\n");
                            Exception e = null;
                            try {
                                Listener.flooDisable();
                                d.replaceString(flooPatchPosition.start, end, contents);
                            } catch (Exception exception) {
                                e = exception;
                            } finally {
                                Listener.flooEnable();
                            }

                            if (e != null) {
                                Flog.throwAHorribleBlinkingErrorAtTheUser(e);
                                FlooHandler.getInstance().send_get_buf(id);
                                return;
                            }

                            for (Editor editor : editors) {
                                if (editor.isDisposed()) {
                                    continue;
                                }
                                CaretModel caretModel = editor.getCaretModel();
                                int offset = caretModel.getOffset();
                                if (offset < flooPatchPosition.start) {
                                    continue;
                                }
                                int newOffset = offset + contents.length() - flooPatchPosition.end;
                                Flog.log("Moving cursor from %s to %s", offset, newOffset);
                                caretModel.moveToOffset(newOffset);
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
