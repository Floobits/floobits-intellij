package floobits.common;

import floobits.common.interfaces.IContext;
import floobits.common.interfaces.IFile;
import floobits.common.protocol.Connection;
import floobits.common.protocol.FlooPatch;
import floobits.common.protocol.buf.Buf;
import floobits.common.protocol.buf.TextBuf;
import floobits.common.protocol.json.receive.*;
import floobits.common.protocol.json.send.*;
import floobits.utilities.Flog;

import java.util.ArrayList;
import java.util.Arrays;

public class OutboundRequestHandler {
    private final IContext context;
    private final FloobitsState state;
    private final Connection conn;
    private FlooHighlight previousHighlight;

    public OutboundRequestHandler(IContext context, FloobitsState state, Connection conn) {
        this.context = context;
        this.state = state;
        this.conn = conn;
    }

    public void getBuf(Integer buf_id) {
        if (state == null || state.bufs == null) {
            Flog.warn("Tried to get buf with no state!?");
            return;
        }
        Buf buf = state.bufs.get(buf_id);
        if (buf == null) {
            return;
        }
        synchronized (buf) {
            buf.set(null, null);
        }
        conn.write(new GetBuf(buf_id));
    }

    public void patch(String textPatch, String before_md5, TextBuf b) {
        if (!state.can("patch")) {
            return;
        }
        if (Buf.isBad(b)) {
            Flog.info("Not sending patch. Buf isn't populated yet %s", b != null ? b.path : "?");
            return;
        }
        Flog.log("Sending patch for %s", b.path);
        FlooPatch req = new FlooPatch(textPatch, before_md5, b);
        conn.write(req);
    }

    void createBuf(IFile virtualFile) {
        Buf buf = Buf.createBuf(virtualFile, context, this);
        if (buf == null) {
            return;
        }
        if (!state.can("patch")) {
            return;
        }
        conn.write(new CreateBuf(buf));
    }

    public void deleteBuf(Buf buf, boolean unlink) {
        if (!state.can("patch")) {
            return;
        }
        buf.cancelTimeout();
        conn.write(new DeleteBuf(buf.id, unlink));
    }

    public void saveBuf(final Buf b) {
        if (Buf.isBad(b)) {
            Flog.info("Not sending save. Buf isn't populated yet %s", b != null ? b.path : "?");
            return;
        }
        if (!state.can("patch")) {
            return;
        }
        Flog.info("Scheduling save");
        context.setTimeout(100, new Runnable() {
            @Override
            public void run() {
                Flog.info("Saving");
                conn.write(new SaveBuf(b.id));
            }
        });

    }

    public void setBuf(Buf b) {
        if (!state.can("patch")) {
            return;
        }
        b.cancelTimeout();
        conn.write(new SetBuf(b));
    }

    public void renameBuf(Buf b, String newRelativePath) {
        if (!state.can("patch")) {
            return;
        }
        b.cancelTimeout();
        state.setBufPath(b, newRelativePath);
        conn.write(new RenameBuf(b.id, b.path));
    }

    public void highlight(Buf b, ArrayList<ArrayList<Integer>> textRanges, boolean summon, boolean following) {
        if (!state.can("highlight")) {
            return;
        }
        if (textRanges.size() == 0) {
            Flog.info("Attempt to send a highlight with no ranges.");
            return;
        }
        if (Buf.isBad(b)) {
            Flog.info("Not sending highlight. Buf isn't populated yet %s", b != null ? b.path : "?");
            return;
        }

        FlooHighlight flooHighlight = new FlooHighlight(b, textRanges, summon, following);
        if (flooHighlight.equals(previousHighlight)) {
            return;
        }
        previousHighlight = flooHighlight;
        conn.write(flooHighlight);
    }

    public void summon(String current, Integer offset, String username) {
        if (!state.can("patch")) {
            return;
        }
        Buf buf = state.getBufByPath(current);
        if (Buf.isBad(buf)) {
            context.errorMessage(String.format("The file %s is not shared. Changes will not be synchronized.", current));
            return;
        }
        ArrayList<ArrayList<Integer>> ranges = new ArrayList<ArrayList<Integer>>();
        ranges.add(new ArrayList<Integer>(Arrays.asList(offset, offset)));
        if (username == null) {
            conn.write(new FlooHighlight(buf, ranges, true, false));
        } else {
            ArrayList<String> to = new ArrayList<String>();
            to.add(username);
            conn.write(new FlooHighlight(buf, ranges, true, false, to));
        }
    }

    public void requestEdit() {
        if (!state.can("request_perms")) {
            context.errorMessage("You are not allowed to ask for edit permissions.");
            return;
        }
        conn.write(new EditRequest(new ArrayList<String>(Arrays.asList("edit_room"))));
    }

    public void message(String chatContents) {
        conn.write(new FlooMessage(chatContents));
    }

    public void kick(int userId) {
        if (!state.can("kick")) {
            return;
        }
        conn.write(new FlooKick(userId));
    }

    public void pong() {
        conn.write(new Pong());
    }

    public void setPerms(String action, int userId, String[] perms) {
        if (!state.can("kick")) {
            return;
        }
        state.changePermsForUser(userId, perms);
        conn.write(new PermsChange(action, userId, perms));
    }
}
