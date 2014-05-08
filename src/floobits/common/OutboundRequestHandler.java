package floobits.common;

import com.intellij.openapi.vfs.VirtualFile;
import floobits.FlooContext;
import floobits.common.protocol.FlooPatch;
import floobits.common.protocol.receive.*;
import floobits.common.protocol.send.*;
import floobits.utilities.Flog;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by kans on 5/7/14.
 */
public class OutboundRequestHandler {
    private final FlooContext context;
    private final FloobitsState state;
    private final Connection conn;
    protected Integer requestId = 0;

    public OutboundRequestHandler(FlooContext context, FloobitsState state, Connection conn) {
        this.context = context;
        this.state = state;
        this.conn = conn;
    }

    public void send_get_buf(Integer buf_id, boolean verify) {
        Buf buf = state.bufs.get(buf_id);
        if (buf == null) {
            return;
        }
        synchronized (buf) {
            buf.set(null, null);
        }
        conn.write(new GetBuf(buf_id), verify ? requestId++ : null);
    }

    public void send_get_buf (Integer buf_id) {
        send_get_buf(buf_id, false);
    }

    public void send_patch (String textPatch, String before_md5, TextBuf buf) {
        if (!state.can("patch")) {
            return;
        }
        Flog.log("Sending patch for %s", buf.path);
        FlooPatch req = new FlooPatch(textPatch, before_md5, buf);
        conn.write(req);
    }

    void send_create_buf(VirtualFile virtualFile) {
        send_create_buf(virtualFile, false);
    }

    void send_create_buf(VirtualFile virtualFile, boolean verify) {
        Buf buf = Buf.createBuf(virtualFile, context, this);
        if (buf == null) {
            return;
        }
        conn.write(new CreateBuf(buf), verify ? requestId++ : null);
    }

    public void send_delete_buf(Buf buf) {
        send_delete_buf(buf, false);
    }

    public void send_delete_buf(Buf buf, boolean unlink) {
        buf.cancelTimeout();
        if (!state.can("patch")) {
            return;
        }
        buf.cancelTimeout();
        conn.write(new DeleteBuf(buf.id, unlink));
    }

    public void send_set_buf(Buf b) {
        send_set_buf(b, false);
    }

    public void send_set_buf (Buf b, boolean verify) {
        if (!state.can("patch")) {
            return;
        }
        conn.write(new SetBuf(b), verify ? requestId++ : null);
    }

    public void send_rename_buf(Buf b, String newRelativePath) {
        if (!state.can("patch")) {
            return;
        }
        b.cancelTimeout();
        state.set_buf_path(b, newRelativePath);
        conn.write(new RenameBuf(b.id, newRelativePath));
    }

    public void send_highlight(Buf b, ArrayList<ArrayList<Integer>> textRanges, boolean summon) {
        if (!state.can("highlight")) {
            return;
        }
        if (Buf.isBad(b)) {
            Flog.info("buf isn't populated yet %s", b.path);
            return;
        }
        conn.write(new FlooHighlight(b, textRanges, summon, state.stalking));
    }

    public void send_summon(String current, Integer offset) {
        if (!state.can("patch")) {
            return;
        }
        Buf buf = state.get_buf_by_path(current);
        if (Buf.isBad(buf)) {
            context.errorMessage(String.format("The file %s is not shared!", current));
            return;
        }
        ArrayList<ArrayList<Integer>> ranges = new ArrayList<ArrayList<Integer>>();
        ranges.add(new ArrayList<Integer>(Arrays.asList(offset, offset)));
        conn.write(new FlooHighlight(buf, ranges, true, state.stalking));
    }

    public void sendEditRequest() {
        if (!state.can("request_perms")) {
            Utils.errorMessage("You are not allowed to ask for edit permissions.", context.project);
            return;
        }
        conn.write(new EditRequest(new ArrayList<String>(Arrays.asList("edit_room"))));
    }

    public void send_save_buf(Buf b) {
        if (Buf.isBad(b)) {
            Flog.info("buf isn't populated yet %s", b.path);
            return;
        }
        if (!state.can("patch")) {
            return;
        }
        conn.write(new SaveBuf(b.id));
    }

    public void send_FlooMessage(String chatContents) {
        conn.write(new FlooMessage(chatContents));
    }

    public void send_kick(int userId) {
        if (!state.can("kick")) {
            return;
        }
        conn.write(new FlooKick(userId));
    }
    public void send_perms_change(int userId, String[] perms) {
        if (!state.can("kick")) {
            return;
        }
        conn.write(new PermsChange("set", userId, perms));
        state.changePermsForUser(userId, perms);
    }

    public void sendPong() {
        conn.write(new Pong());
    }

    public void send_set_perms(String action, int userId, String[] perms) {
        if (!state.can("kick")) {
            return;
        }
        conn.write(new PermsChange(action, userId, perms));
    }
}
