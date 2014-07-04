package floobits.common;

import floobits.FlooContext;
import floobits.common.dmp.FlooDmp;
import floobits.common.dmp.FlooPatchPosition;
import floobits.common.dmp.diff_match_patch;
import floobits.common.protocol.FlooPatch;
import floobits.utilities.Flog;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.LinkedList;
import java.util.List;

abstract public class TextBuf<T> extends Buf<String, T> {
    protected static FlooDmp dmp = new FlooDmp();

    public TextBuf (String path, Integer id, String buf, String md5, FlooContext context, OutboundRequestHandler outbound) {
        super(path, id, buf, md5, context, outbound);
        if (buf != null) {
            this.buf = NEW_LINE.matcher(buf).replaceAll("\n");
        }
        this.encoding = Encoding.UTF8;
    }


    public abstract String getText();
    protected abstract String getText(T f);
    protected abstract String getViewText();
    protected abstract Object[] applyPatch(FlooPatchPosition[] positions);
    protected abstract void updateScroll(Object context);

    public void read () {
        String text = getText();
        if (text == null) {
            return;
        }

        this.buf = text;
        this.md5 = DigestUtils.md5Hex(text);
    }

    public void write() {
        if (!isPopulated()) {
            Flog.warn("Unable to write %s because it's not populated yet.", path);
            return;
        }
        updateView();
    }

    synchronized public void set(String s, String newMD5) {
        buf = s == null ? null : NEW_LINE.matcher(s).replaceAll("\n");
        md5 = newMD5;
    }

    public String serialize() {
        return buf;
    }

    public void send_patch(T f) {
        send_patch(getText(f));
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

    protected void getBuf() {
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
        Flog.info("Got _on_patch");

        String md5FromDoc;
        String viewText = getViewText();
        if (viewText == null) {
            return;
        }
        cancelTimeout();

        String md5Before = DigestUtils.md5Hex(viewText);
        if (!md5Before.equals(res.md5_before)) {
            Flog.warn("starting md5s don't match for %s. this is dangerous!", path);
        }

        List<diff_match_patch.Patch> patches =  dmp.patch_fromText(res.patch);
        final Object[] results = dmp.patch_apply((LinkedList<diff_match_patch.Patch>) patches, viewText);
        final String patchedContents = (String) results[0];
        final boolean[] patchesClean = (boolean[]) results[1];
        final FlooPatchPosition[] positions = (FlooPatchPosition[]) results[2];

        for (boolean clean : patchesClean) {
            if (!clean) {
                Flog.log("Patch not clean for %s. Sending get_buf and setting readonly.", path);
                getBuf();
                return;
            }
        }
        Object[] patch_res = applyPatch(positions);
        if (patch_res == null) {
            return;
        }
        String text = (String) patch_res[0];


        // XXX: If patchedContents have carriage returns this will be a problem:
        String md5After = DigestUtils.md5Hex(patchedContents);
        if (!md5After.equals(res.md5_after)) {
            Flog.info("MD5 after mismatch (ours %s remote %s)", md5After, res.md5_after);
        }

        md5FromDoc = DigestUtils.md5Hex(text);
        if (!md5FromDoc.equals(res.md5_after)) {
            Flog.info("md5FromDoc mismatch (ours %s remote %s)", md5FromDoc, res.md5_after);
            setGetBufTimeout();
        }

        updateScroll(patch_res[1]);

        set(text, md5FromDoc);
        Flog.log("Patched %s", res.path);
    }
}
