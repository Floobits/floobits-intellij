package floobits.common.protocol.buf;

import floobits.common.Constants;
import floobits.common.Encoding;
import floobits.common.OutboundRequestHandler;
import floobits.common.dmp.FlooDmp;
import floobits.common.dmp.FlooPatchPosition;
import floobits.common.dmp.diff_match_patch;
import floobits.common.interfaces.IContext;
import floobits.common.interfaces.IDoc;
import floobits.common.interfaces.IFile;
import floobits.common.protocol.FlooPatch;
import floobits.utilities.Flog;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.LinkedList;
import java.util.List;


public class TextBuf extends Buf<String> {
    protected static FlooDmp dmp = new FlooDmp();

    public TextBuf(String path, Integer id, String buf, String md5, IContext context, OutboundRequestHandler outbound) {
        super(path, id, buf, md5, context, outbound);
        if (buf != null) {
            this.buf = Constants.NEW_LINE.matcher(buf).replaceAll("\n");
        }
        this.encoding = Encoding.UTF8;
    }


    public void read () {
        IDoc d = getVirtualDoc();
        if (d == null) {
            return;
        }
        this.buf = d.getText();
        this.md5 = DigestUtils.md5Hex(this.buf);
    }

    public void write() {
        if (!isPopulated()) {
            Flog.warn("Unable to write %s because it's not populated yet.", path);
            return;
        }

        IDoc d = getVirtualDoc();
        if (d != null) {
            synchronized (context) {
                try {
                    context.setListener(false);
                    d.setReadOnly(false);
                    d.setText(buf);
                } finally {
                    context.setListener(true);
                }
                return;
            }
        }

        Flog.warn("Tried to write to null document: %s", path);

        IFile virtualFile = getOrCreateFile();
        try {
            virtualFile.setBytes(buf.getBytes());
        } catch (Throwable e) {
            Flog.warn(e);
            context.errorMessage("The Floobits plugin was unable to write to a file.");
        }
    }

    synchronized public void set(String s, String newMD5) {
        buf = s == null ? null : Constants.NEW_LINE.matcher(s).replaceAll("\n");
        md5 = newMD5;
    }

    public String serialize() {
        return buf;
    }

    @Override
    public void send_patch(IFile virtualFile) {
        IDoc d = context.iFactory.getDocument(virtualFile);
        if (d == null) {
            Flog.warn("Can't get document to read from disk for sending patch %s", path);
            return;
        }
        send_patch(d.getText());
    }

    public void send_patch(String current) {

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
       outbound.patch(textPatch, before_md5, this);
    }

    private void getBuf() {
        cancelTimeout();
        outbound.getBuf(id);
    }

    private void setGetBufTimeout() {
        final int buf_id = id;
        cancelTimeout();
        timeout = context.setTimeout(2000, new Runnable() {
            @Override
            public void run() {
                Flog.info("Sending get buf after timeout.");
                outbound.getBuf(buf_id);
            }
        });
    }

    public void patch(final FlooPatch res) {
        final TextBuf b = this;
        Flog.info("Got _on_patch");

        String oldText = buf;
        IFile virtualFile = b.getVirtualFile();
        if (virtualFile == null) {
            Flog.warn("VirtualFile is null, no idea what do do. Aborting everything %s", this);
            getBuf();
            return;
        }
        IDoc d = context.iFactory.getDocument(virtualFile);
        if (d == null) {
            Flog.warn("Document not found for %s", virtualFile);
            getBuf();
            return;
        }
        String viewText;
        if (!virtualFile.exists()) {
            viewText = oldText;
        } else {
            viewText = d.getText();
            if (viewText.equals(oldText)) {
                b.forced_patch = false;
            } else if (!b.forced_patch) {
                b.forced_patch = true;
                oldText = viewText;
                b.send_patch(viewText);
                Flog.warn("Sending force patch for %s. this is dangerous!", b.path);
            }
        }

        b.cancelTimeout();

        String md5Before = DigestUtils.md5Hex(viewText);
        if (!md5Before.equals(res.md5_before)) {
            Flog.warn("starting md5s don't match for %s. this is dangerous!", b.path);
        }

        List<diff_match_patch.Patch> patches =  dmp.patch_fromText(res.patch);
        final Object[] results = dmp.patch_apply((LinkedList<diff_match_patch.Patch>) patches, oldText);
        final String patchedContents = (String) results[0];
        final boolean[] patchesClean = (boolean[]) results[1];
        final FlooPatchPosition[] positions = (FlooPatchPosition[]) results[2];

        for (boolean clean : patchesClean) {
            if (!clean) {
                Flog.log("Patch not clean for %s. Sending get_buf and setting readonly.", d);
                getBuf();
                return;
            }
        }
        // XXX: If patchedContents have carriage returns this will be a problem:
        String md5After = DigestUtils.md5Hex(patchedContents);
        if (!md5After.equals(res.md5_after)) {
            Flog.info("MD5 after mismatch (ours %s remote %s)", md5After, res.md5_after);
        }

        if (!d.makeWritable()) {
            Flog.info("Document: %s is not writable.", d);
            return;
        }

        String text = d.patch(positions);
        if (text == null) {
            getBuf();
            return;
        }

        String md5FromDoc = DigestUtils.md5Hex(text);
        if (!md5FromDoc.equals(res.md5_after)) {
            Flog.info("md5FromDoc mismatch (ours %s remote %s)", md5FromDoc, res.md5_after);
            b.setGetBufTimeout();
        }

        b.set(text, md5FromDoc);
        Flog.log("Patched %s", res.path);
    }
}
