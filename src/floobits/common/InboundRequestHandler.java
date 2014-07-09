package floobits.common;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import floobits.common.interfaces.FlooContext;
import floobits.Listener;
import floobits.common.interfaces.VDoc;
import floobits.common.interfaces.VFile;
import floobits.common.protocol.FlooPatch;
import floobits.common.protocol.FlooUser;
import floobits.common.protocol.receive.*;
import floobits.common.protocol.send.CreateBufResponse;
import floobits.common.protocol.send.RoomInfoResponse;
import floobits.dialogs.DisconnectNoticeDialog;
import floobits.dialogs.HandleRequestPermsRequestDialog;
import floobits.dialogs.HandleTooBigDialog;
import floobits.dialogs.ResolveConflictsDialog;
import floobits.utilities.Flog;
import floobits.utilities.ThreadSafe;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.util.*;


public class InboundRequestHandler {
    private FlooContext context;
    private final FloobitsState state;
    private final OutboundRequestHandler outbound;
    private boolean shouldUpload;
    private EditorScheduler editor;

    enum Events {
        room_info, get_buf, patch, highlight, saved, join, part, create_buf, ack,
        request_perms, msg, rename_buf, term_stdin, term_stdout, delete_buf, perms, ping
    }
    public InboundRequestHandler(FlooContext context, FloobitsState state, OutboundRequestHandler outbound, boolean shouldUpload) {
        this.context = context;
        editor = context.editor;
        this.state = state;
        this.outbound = outbound;
        this.shouldUpload = shouldUpload;
    }

    private void initialManageConflicts(RoomInfoResponse ri) {
        final LinkedList<Buf> conflicts = new LinkedList<Buf>();
        final LinkedList<Buf> missing = new LinkedList<Buf>();
        final LinkedList<String> conflictedPaths = new LinkedList<String>();
        for (Map.Entry entry : ri.bufs.entrySet()) {
            Integer buf_id = (Integer) entry.getKey();
            RoomInfoBuf b = (RoomInfoBuf) entry.getValue();
            Buf buf = Buf.createBuf(b.path, b.id, Encoding.from(b.encoding), b.md5, context, outbound);
            state.bufs.put(buf_id, buf);
            state.paths_to_ids.put(b.path, b.id);
            buf.read();
            if (buf.buf == null) {
                if (buf.path.equals("FLOOBITS_README.md") && buf.id == 1) {
                    outbound.getBuf(buf.id);
                    continue;
                }
                missing.add(buf);
                conflictedPaths.add(buf.path);
                continue;
            }
            if (!b.md5.equals(buf.md5)) {
                conflicts.add(buf);
                conflictedPaths.add(buf.path);
            }
        }

        if (conflictedPaths.size() <= 0) {
            return;
        }
        String[] conflictedPathsArray = conflictedPaths.toArray(new String[conflictedPaths.size()]);
        ResolveConflictsDialog dialog = new ResolveConflictsDialog(
                new Runnable() {
                    @Override
                    public void run() {
                        for (Buf buf : conflicts) {
                            outbound.getBuf(buf.id);
                        }
                        for (Buf buf : missing) {
                            outbound.getBuf(buf.id);
                        }
                    }
                }, new Runnable() {
            @Override
            public void run() {
                for (Buf buf : conflicts) {
                    outbound.setBuf(buf);
                }
                for (Buf buf : missing) {
                    outbound.deleteBuf(buf, false);
                }
            }
        }, state.readOnly,
                new Runnable() {
                    @Override
                    public void run() {
                        context.shutdown();
                    }
                }, conflictedPathsArray
        );
        dialog.createCenterPanel();
        dialog.show();
    }
    private void initialUpload(RoomInfoResponse ri) {
        context.statusMessage("Overwriting remote files and uploading new ones.");
        context.flashMessage("Overwriting remote files and uploading new ones.");

        final Ignore ignoreTree = context.getIgnoreTree();
        ArrayList<Ignore> allIgnores = new ArrayList<Ignore>();
        LinkedList<Ignore> tempIgnores = new LinkedList<Ignore>();
        tempIgnores.add(ignoreTree);
        int size = 0;
        Ignore ignore;
        while (tempIgnores.size() > 0) {
            ignore = tempIgnores.removeLast();
            size += ignore.size;
            allIgnores.add(ignore);
            for(Ignore ig: ignore.children.values()) {
                tempIgnores.add(ig);
            }
        }
        LinkedList<Ignore> tooBigIgnores = new LinkedList<Ignore>();
        Collections.sort(allIgnores);

        while (size > ri.max_size) {
            Ignore ig = allIgnores.remove(0);
            size -= ig.size;
            tooBigIgnores.add(ig);
        }
        if (tooBigIgnores.size() > 0) {
            int TOO_MANY_BIG_DIRS = 50;
            if (tooBigIgnores.size() > TOO_MANY_BIG_DIRS) {
                NumberFormat numberFormat = NumberFormat.getNumberInstance();
                String howMany = numberFormat.format(tooBigIgnores.size());
                String tooMuch = numberFormat.format(ri.max_size/1000);
                String notice = String.format("You have too many directories that are over %s MB to upload with Floobits.", tooMuch);
                DisconnectNoticeDialog disconnectNoticeDialog = new DisconnectNoticeDialog(new Runnable() {
                    @Override
                    public void run() {
                        context.shutdown();
                    }
                }, String.format("%s We limit it to %d and you have %s big directories.", notice, TOO_MANY_BIG_DIRS, howMany));
                disconnectNoticeDialog.createCenterPanel();
                disconnectNoticeDialog.show();
                return;
            }
            final Boolean[] shouldContinue = new Boolean[1];
            // shouldContinue[0] is null when user closes dialog instead of clicking a button:
            shouldContinue[0] = false;
            HandleTooBigDialog handleTooBigDialog = new HandleTooBigDialog(new RunLater<Boolean>() {
                @Override
                public void run(Boolean arg) {
                    shouldContinue[0] = arg;
                }
            }, tooBigIgnores);

            handleTooBigDialog.createCenterPanel();
            handleTooBigDialog.show();

            if (!shouldContinue[0]) {
                context.shutdown();
                return;
            }
        }

        HashSet<String> paths = new HashSet<String>();
        for (Ignore ig : allIgnores) {
            for (VFile virtualFile : ig.files)
                paths.add(context.toProjectRelPath(virtualFile.getPath()));
        }
        for (Map.Entry entry : ri.bufs.entrySet()) {
            Integer buf_id = (Integer) entry.getKey();
            RoomInfoBuf b = (RoomInfoBuf) entry.getValue();
            Buf buf = Buf.createBuf(b.path, b.id, Encoding.from(b.encoding), b.md5, context, outbound);
            state.bufs.put(buf_id, buf);
            state.paths_to_ids.put(b.path, b.id);
            if (!paths.contains(buf.path)) {
                outbound.deleteBuf(buf, false);
                continue;
            }
            paths.remove(buf.path);
            buf.read();
            if (buf.buf == null) {
                outbound.getBuf(buf.id);
                continue;
            }
            if (b.md5.equals(buf.md5)) {
                continue;
            }
            outbound.setBuf(buf);
        }


        for (String path : paths) {
            VFile fileByPath = context.vFactory.findFileByPath(context.absPath(path));
            if (fileByPath == null || !fileByPath.isValid()) {
                Flog.warn(String.format("path is no longer a valid virtual file"));
                continue;
            }
            outbound.createBuf(fileByPath);
        }
        String flooignore = FilenameUtils.concat(context.colabDir, ".flooignore");

        try {
            File f = new File(flooignore);
            List<String> strings;
            if (f.exists()) {
                strings = FileUtils.readLines(f);
            } else {
                strings = new ArrayList<String>();
            }

            for (Ignore ig : tooBigIgnores) {
                String rule = "/" + context.toProjectRelPath(ig.stringPath);
                if (!rule.endsWith("/")) {
                    rule += "/";
                }
                rule += "*";
                strings.add(rule);
            }
            Listener.flooDisable();
            FileUtils.writeLines(f, strings);
            VFile fileByIoFile = context.vFactory.findFileByIoFile(f);
            if (fileByIoFile != null) {
                fileByIoFile.refresh();
                ignoreTree.addRules(fileByIoFile);
            }
        } catch (IOException e) {
            Flog.warn(e);
        } finally {
            Listener.flooEnable();
        }
        shouldUpload = false;
    }
    void _on_rename_buf(JsonObject jsonObject) {
        final String name = jsonObject.get("old_path").getAsString();
        final String oldPath = context.absPath(name);
        final String newPath = context.absPath(jsonObject.get("path").getAsString());

        Buf buf = state.get_buf_by_path(oldPath);
        if (buf == null) {
            if (state.get_buf_by_path(newPath) == null) {
                Flog.warn("Rename oldPath and newPath don't exist. %s %s", oldPath, newPath);
            } else {
                Flog.info("We probably rename this, nothing to rename.");
            }
            return;
        }

        editor.queue(buf, new RunLater<Buf>() {
                @Override
                public void run(Buf buf) {
                    final VFile foundFile = context.vFactory.findFileByPath(oldPath);
                    if (foundFile == null) {
                        Flog.warn("File we want to move was not found %s %s.", oldPath, newPath);
                        return;
                    }
                    String newRelativePath = context.toProjectRelPath(newPath);
                    if (newRelativePath == null) {
                        context.errorMessage("A file is now outside the workspace.");
                        return;
                    }
                    state.set_buf_path(buf, newRelativePath);

                    File oldFile = new File(oldPath);
                    File newFile = new File(newPath);
                    String newFileName = newFile.getName();
                    // Rename file

                    if (foundFile.rename(null, newFileName)) {
                        return;
                    }

                    // Move file
                    String newParentDirectoryPath = newFile.getParent();
                    String oldParentDirectoryPath = oldFile.getParent();
                    if (newParentDirectoryPath.equals(oldParentDirectoryPath)) {
                        Flog.warn("Only rename file, don't need to move %s %s", oldPath, newPath);
                        return;
                    }
                    VFile directory = context.vFactory.createDirectories(newParentDirectoryPath);
                    if (directory == null) {
                        return;
                    }

                    foundFile.move(null, directory);
                }
            }
        );
    }

    void _on_request_perms(JsonObject obj) {
        Flog.log("got perms receive %s", obj);
        RequestPerms requestPerms = new Gson().fromJson(obj, (Type)RequestPerms.class);
        final int userId = requestPerms.user_id;
        final FlooUser u = state.getUser(userId);
        if (u == null) {
            Flog.info("Unknown user for id %s. Not handling request_perms event. %d", userId);
            return;
        }
        ThreadSafe.later(new Runnable() {
            @Override
            public void run() {
                HandleRequestPermsRequestDialog d = new HandleRequestPermsRequestDialog(u.username, context, new RunLater<String>() {
                    @Override
                    public void run(String action) {
                        String[] perms = new String[]{"edit_room"};
                        outbound.setPerms(action, userId, perms);
                    }
                });
                d.createCenterPanel();
                d.show();
            }
        });
    }

    void _on_join(JsonObject obj) {
        FlooUser u = new Gson().fromJson(obj, (Type) FlooUser.class);
        state.addUser(u);
    }

    void _on_part(JsonObject obj) {
        JsonElement id = obj.get("user_id");
        if (id == null){
            return;
        }
        Integer userId = id.getAsInt();
        state.removeUser(userId);
        context.vFactory.removeHighlightsForUser(userId);
    }

    void _on_delete_buf(JsonObject obj) {
        final DeleteBuf deleteBuf = new Gson().fromJson(obj, (Type)DeleteBuf.class);
        Buf buf = state.bufs.get(deleteBuf.id);
        if (buf == null) {
            Flog.warn(String.format("Tried to delete a buf that doesn't exist: %s", deleteBuf.id));
            return;
        }
        editor.queue(buf, new RunLater<Buf>() {
            @Override
            public void run(Buf buf) {
                buf.cancelTimeout();
                if (state.bufs != null) {
                    state.bufs.remove(deleteBuf.id);
                    state.paths_to_ids.remove(buf.path);
                }
                if (!deleteBuf.unlink) {
                    context.statusMessage(String.format("Removed the file, %s, from the workspace.", buf.path));
                    return;
                }
                String absPath = context.absPath(buf.path);
                final VFile fileByPath = context.vFactory.findFileByPath(absPath);

                if (fileByPath == null) {
                    return;
                }

                fileByPath.delete(this);
            }
        });
    }

    void _on_msg(JsonObject jsonObject){
        String msg = jsonObject.get("data").getAsString();
        String username = jsonObject.get("username").getAsString();
        Double time = jsonObject.get("time").getAsDouble();
        Date messageDate;
        if (time == null) {
            messageDate = new Date();
        } else {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(time.longValue() * 1000);
            messageDate = c.getTime();
        }

        context.chat(username, msg, messageDate);
    }

    void _on_term_stdout(JsonObject jsonObject) {}
    void _on_term_stdin(JsonObject jsonObject) {}

    void _on_ping(JsonObject jsonObject) {
        outbound.pong();
    }

    public void _on_highlight(JsonObject obj) {
        final FlooHighlight res = new Gson().fromJson(obj, (Type) FlooHighlight.class);
        final Boolean force = (state.stalking && !res.following) || res.ping || (res.summon == null ? Boolean.FALSE : res.summon);
        state.lastHighlight = obj;
        final Buf buf = this.state.bufs.get(res.id);
        editor.queue(buf, new RunLater<Buf>() {
            @Override
            public void run(Buf arg) {
                VDoc vDoc = context.vFactory.getDocument(buf.path);
                if (vDoc == null) {
                    return;
                }
                vDoc.applyHighlight(buf.path, res.user_id, state.getUsername(res.user_id), force, res.ranges);
            }
        });
    }

    void _on_saved(JsonObject obj) {
        final Integer id = obj.get("id").getAsInt();
        final Buf buf = this.state.bufs.get(id);
        editor.queue(buf, new RunLater<Buf>() {
            public void run(Buf b) {
                VDoc document = context.vFactory.getDocument(buf.path);
                if (document == null) {
                    return;
                }
                document.save();
            }
        });
    }

    void _on_create_buf(JsonObject obj) {
        Gson gson = new Gson();
        GetBufResponse res = gson.fromJson(obj, (Type) CreateBufResponse.class);
        Buf buf;
        if (res.encoding.equals(Encoding.BASE64.toString())) {
            buf = new BinaryBuf(res.path, res.id, new Base64().decode(res.buf.getBytes()), res.md5, context, outbound);
        } else {
            buf = new TextBuf(res.path, res.id, res.buf, res.md5, context, outbound);
        }
        editor.queue(buf, new RunLater<Buf>() {
            @Override
            public void run(Buf buf) {
                if (state.bufs == null) {
                    return;
                }
                state.bufs.put(buf.id, buf);
                state.paths_to_ids.put(buf.path, buf.id);
                buf.write();
                context.statusMessage(String.format("Added the file, %s, to the workspace.", buf.path));
            }
        });
    }

    void _on_perms(JsonObject obj) {
        Perms res = new Gson().fromJson(obj, (Type) Perms.class);

        Boolean previousState = state.can("patch");
        if (res.user_id != state.getMyConnectionId()) {
            return;
        }
        HashSet perms = new HashSet<String>(Arrays.asList(res.perms));
        if (res.action.equals("add")) {
            state.perms.addAll(perms);
        } else if (res.action.equals("set")) {
            state.perms.clear();
            state.perms.addAll(perms);
        } else if (res.action.equals("remove")) {
            state.perms.removeAll(perms);
        }
        state.readOnly = !state.can("patch");
        if (state.can("patch") != previousState) {
            if (state.can("patch")) {
                context.statusMessage("You state.can now edit this workspace.");
                context.vFactory.clearReadOnlyState();
            } else {
                context.errorMessage("You state.can no longer edit this workspace.");
            }
        }
    }

    void _on_patch(JsonObject obj) {
        final FlooPatch res = new Gson().fromJson(obj, (Type) FlooPatch.class);
        final Buf buf = this.state.bufs.get(res.id);
        editor.queue(buf, new RunLater<Buf>() {
            @Override
            public void run(Buf b) {
                if (b.buf == null) {
                    Flog.warn("no buffer");
                    outbound.getBuf(res.id);
                    return;
                }

                if (res.patch.length() == 0) {
                    Flog.warn("wtf? no patches to apply. server is being stupid");
                    return;
                }
                b.patch(res);
            }
        });
    }
    void _on_room_info(final JsonObject obj) {
        ThreadSafe.read(context, new Runnable() {
            @Override
            public void run() {
                try{
                    RoomInfoResponse ri = new Gson().fromJson(obj, (Type) RoomInfoResponse.class);
                    state.handleRoomInfo(ri);

                    context.statusMessage(String.format("You successfully joined %s ", state.url.toString()));
                    context.openChat();

                    DotFloo.write(context.colabDir, state.url.toString());

                    if (shouldUpload) {
                        if (!state.readOnly) {
                            initialUpload(ri);
                            return;
                        }
                        context.statusMessage("You don't have permission to update remote files.");
                    }
                    initialManageConflicts(ri);
                } catch (Throwable e) {
                    API.uploadCrash(context, e);
                    context.errorMessage("There was a critical error in the plugin" + e.toString());
                    context.shutdown();
                }
            }
        });
    }

    void _on_get_buf(JsonObject obj) {
        Gson gson = new Gson();
        final GetBufResponse res = gson.fromJson(obj, (Type) GetBufResponse.class);
        Buf b = state.bufs.get(res.id);
        editor.queue(b, new RunLater<Buf>() {
            @Override
            public void run(Buf b) {
                b.set(res.buf, res.md5);
                b.write();
                Flog.info("on get buffed. %s", b.path);
            }
        });
    }

    public void on_data(String name, JsonObject obj) {
        Events event;

        try {
            event = Events.valueOf(name);
        } catch (IllegalArgumentException e) {
            Flog.log("No enum for %s", name);
            return;
        }
        switch (event) {
            case room_info:
                _on_room_info(obj);
                break;
            case get_buf:
                _on_get_buf(obj);
                break;
            case patch:
                _on_patch(obj);
                break;
            case highlight:
                _on_highlight(obj);
                break;
            case saved:
                _on_saved(obj);
                break;
            case join:
                _on_join(obj);
                break;
            case part:
                _on_part(obj);
                break;
            case create_buf:
                _on_create_buf(obj);
                break;
            case request_perms:
                _on_request_perms(obj);
                break;
            case msg:
                _on_msg(obj);
                break;
            case rename_buf:
                _on_rename_buf(obj);
                break;
            case term_stdin:
                _on_term_stdin(obj);
                break;
            case term_stdout:
                _on_term_stdout(obj);
                break;
            case delete_buf:
                _on_delete_buf(obj);
                break;
            case perms:
                _on_perms(obj);
                break;
            case ping:
                _on_ping(obj);
                break;
            case ack:
                break;
            default:
                Flog.log("No handler for %s", name);
        }
    }
}
