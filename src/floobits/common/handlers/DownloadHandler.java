package floobits.common.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import floobits.BaseContext;
import floobits.common.*;
import floobits.common.protocol.receive.GetBufResponse;
import floobits.common.protocol.receive.RoomInfoBuf;
import floobits.common.protocol.send.RoomInfoResponse;
import floobits.utilities.Flog;
import floobits.utilities.ThreadSafe;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

public class DownloadHandler extends FlooHandler {
    final HashSet<Integer> missing = new HashSet<Integer>();
    private final Runnable onDownloaded;

    public DownloadHandler(BaseContext context, FlooUrl flooUrl, Runnable onDownloaded) {
        super(context, flooUrl, false);
        this.onDownloaded = onDownloaded;
        readOnly = true;
    }

    void _on_room_info(final JsonObject obj) {
        ThreadSafe.read(context, new Runnable() {
            @Override
            public void run() {
                RoomInfoResponse ri = new Gson().fromJson(obj, (Type) RoomInfoResponse.class);
                isJoined = true;
                users = ri.users;
                perms = new HashSet<String>(Arrays.asList(ri.perms));
                connectionId = Integer.parseInt(ri.user_id);
                Flog.info("Got roominfo with userId %d", connectionId);

                DotFloo.write(context.colabDir, url.toString());

                for (Map.Entry entry : ri.bufs.entrySet()) {
                    Integer buf_id = (Integer) entry.getKey();
                    RoomInfoBuf b = (RoomInfoBuf) entry.getValue();
                    Buf buf = Buf.createBuf(b.path, b.id, Encoding.from(b.encoding), b.md5, context);
                    bufs.put(buf_id, buf);
                    paths_to_ids.put(b.path, b.id);
                    buf.read();
                    if (buf.buf == null) {
                        missing.add(buf.id);
                        send_get_buf(buf.id);
                    }
                }
            }
        });
    }

    void _on_get_buf(JsonObject obj) {
        Gson gson = new Gson();
        final GetBufResponse res = gson.fromJson(obj, (Type) GetBufResponse.class);
        Buf b = bufs.get(res.id);
        queue(b, new RunLater<Buf>() {
            @Override
            public void run(Buf b) {
                b.set(res.buf, res.md5);
                b.write();
                Flog.info("on get buffed. %s", b.path);
                if (missing.contains(b.id)) {
                    missing.remove(b.id);
                    if (missing.isEmpty()) {
                        onDownloaded.run();
                    }
                }
            }
        });
    }
}