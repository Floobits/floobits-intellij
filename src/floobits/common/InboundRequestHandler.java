package floobits.common;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import floobits.GitUtils;
import floobits.common.interfaces.IContext;
import floobits.common.interfaces.IDoc;
import floobits.common.interfaces.IFile;
import floobits.common.protocol.FlooPatch;
import floobits.common.protocol.FlooUser;
import floobits.common.protocol.buf.BinaryBuf;
import floobits.common.protocol.buf.Buf;
import floobits.common.protocol.buf.TextBuf;
import floobits.common.protocol.json.receive.*;
import floobits.common.protocol.json.send.CreateBufResponse;
import floobits.common.protocol.json.send.RoomInfoResponse;
import floobits.utilities.Flog;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;


public class InboundRequestHandler {
    private IContext context;
    private final FloobitsState state;
    private final OutboundRequestHandler outbound;
    private boolean shouldUpload;
    private IFile dirToAdd;
    private StatusMessageThrottler fileAddedMessageThrottler;
    private StatusMessageThrottler fileRemovedMessageThrottler;
    private EditorScheduler editor;

    enum Events {
        room_info, get_buf, patch, highlight, saved, join, part, create_buf, ack,
        request_perms, msg, rename_buf, term_stdin, term_stdout, delete_buf, perms, ping
    }
    public InboundRequestHandler(IContext context, FloobitsState state, OutboundRequestHandler outbound,
                                 boolean shouldUpload,  IFile dirToAdd) {
        this.context = context;
        editor = context.editor;
        this.state = state;
        this.outbound = outbound;
        this.shouldUpload = shouldUpload;
        this.dirToAdd = dirToAdd;
        fileAddedMessageThrottler = new StatusMessageThrottler(context,
                "%d files were added to the workspace.");
        fileRemovedMessageThrottler = new StatusMessageThrottler(context,
                "%d files were removed from the workspace.");
    }

    private void initialManageConflicts(RoomInfoResponse ri) {
        final LinkedList<Buf> conflicts = new LinkedList<Buf>();
        final LinkedList<Buf> missing = new LinkedList<Buf>();
        final LinkedList<String> conflictedPaths = new LinkedList<String>();
        for (Map.Entry entry : ri.bufs.entrySet()) {
            Integer buf_id = (Integer) entry.getKey();
            RoomInfoBuf b = (RoomInfoBuf) entry.getValue();
            Buf buf = Buf.createBuf(b.path, b.id, Encoding.from(b.encoding), b.md5, context, outbound);
            if (state.bufs == null) {
                Flog.warn("Buffer list became null. Probably disconnected. Bailing.");
                return;
            }
            state.bufs.put(buf_id, buf);
            state.pathsToIds.put(b.path, b.id);
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
        Runnable stompLocal = new Runnable() {
            @Override
            public void run() {
                for (Buf buf : conflicts) {
                    outbound.getBuf(buf.id);
                }
                for (Buf buf : missing) {
                    outbound.getBuf(buf.id);
                }
            }
        };
        Runnable stompRemote = new Runnable() {
            @Override
            public void run() {
                for (Buf buf : conflicts) {
                    outbound.setBuf(buf);
                    outbound.saveBuf(buf);
                }
                for (Buf buf : missing) {
                    outbound.deleteBuf(buf, false);
                }
            }
        };
        Runnable flee = new Runnable() {
            @Override
            public void run() {
                context.shutdown();
            }
        };
        LinkedList<String> connectedUsersList = new LinkedList<String>();
        for (Map.Entry<Integer, FlooUser> userEntry : state.users.entrySet()) {
            FlooUser user = userEntry.getValue();
            connectedUsersList.add(String.format("%s, %s", user.username, user.client));
        }
        context.dialogResolveConflicts(stompLocal, stompRemote, state.readOnly, flee, conflictedPathsArray,
                connectedUsersList.toArray(new String[connectedUsersList.size()]));
    }

    private void initialUpload(RoomInfoResponse ri) {
        context.statusMessage("Overwriting remote files and uploading new ones.");
        context.flashMessage("Overwriting remote files and uploading new ones.");

        final Ignore ignoreTree;
        if (dirToAdd == null) {
            ignoreTree = context.getIgnoreTree();
        } else {
            ignoreTree = Ignore.BuildIgnore(dirToAdd);
        }
        Ignore.UploadData uploadData = ignoreTree.getUploadData(ri.max_size, new Utils.FileProcessor<String>() {
            @Override
            public String call(IFile file) {
                return context.toProjectRelPath(file.getPath());
            }
        });
        if (uploadData.bigStuff.size() > 0) {
            if (uploadData.bigStuff.size() > Constants.TOO_MANY_BIG_DIRS) {
                context.dialogDisconnect(ri.max_size / 1000, uploadData.bigStuff.size());
                return;
            }
            boolean shouldContinue;

            shouldContinue = context.dialogTooBig(uploadData.bigStuff);

            if (!shouldContinue) {
                context.shutdown();
                return;
            }
        }
        for (Map.Entry entry : ri.bufs.entrySet()) {
            Integer buf_id = (Integer) entry.getKey();
            RoomInfoBuf b = (RoomInfoBuf) entry.getValue();
            Buf buf = Buf.createBuf(b.path, b.id, Encoding.from(b.encoding), b.md5, context, outbound);
            try {
                state.bufs.put(buf_id, buf);
            } catch (NullPointerException e) {
                Flog.warn("state.bufs is null, tried to upload after disconnecting. This is a race condition.");
                return;
            }
            state.pathsToIds.put(b.path, b.id);
            if (!uploadData.paths.contains(buf.path)) {
                outbound.deleteBuf(buf, false);
                continue;
            }
            uploadData.paths.remove(buf.path);
            buf.read();
            if (buf.buf == null) {
                Flog.warn("%s is null but we want to upload it?", b.path);
                outbound.getBuf(buf.id);
                continue;
            }
            if (b.md5.equals(buf.md5)) {
                continue;
            }
            outbound.setBuf(buf);
            outbound.saveBuf(buf);
        }


        for (String path : uploadData.paths) {
            IFile fileByPath = context.iFactory.findFileByPath(context.absPath(path));
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

            for (Map.Entry<String, Integer> bigData : uploadData.bigStuff.entrySet()) {
                String rule = "/" + context.toProjectRelPath(FilenameUtils.separatorsToUnix(bigData.getKey()));
                if (!rule.endsWith("/")) {
                    rule += "/";
                }
                rule += "*";
                strings.add(rule);
            }
            context.setListener(false);
            FileUtils.writeLines(f, strings);
            IFile fileByIoFile = context.iFactory.findFileByIoFile(f);
            if (fileByIoFile != null) {
                fileByIoFile.refresh();
                ignoreTree.addRules(fileByIoFile);
            }
        } catch (IOException e) {
            Flog.error(e);
        } finally {
            context.setListener(true);
        }
        shouldUpload = false;
        dirToAdd = null;
    }

    void _on_rename_buf(JsonObject jsonObject) {
        final String name = jsonObject.get("old_path").getAsString();
        final String oldPath = context.absPath(name);
        final String newPath = context.absPath(jsonObject.get("path").getAsString());

        Buf buf = state.getBufByPath(oldPath);
        if (buf == null) {
            if (state.getBufByPath(newPath) == null) {
                Flog.warn("Rename oldPath and newPath don't exist. %s %s", oldPath, newPath);
            } else {
                Flog.info("We probably rename this, nothing to rename.");
            }
            return;
        }

        editor.queue(buf, new RunLater<Buf>() {
                    @Override
                    public void run(Buf buf) {
                        final IFile foundFile = context.iFactory.findFileByPath(oldPath);
                        if (foundFile == null) {
                            Flog.warn("File we want to move was not found %s %s.", oldPath, newPath);
                            return;
                        }
                        String newRelativePath = context.toProjectRelPath(newPath);
                        if (newRelativePath == null) {
                            context.errorMessage("A file is now outside the workspace.");
                            return;
                        }
                        state.setBufPath(buf, newRelativePath);

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
                        IFile directory = context.iFactory.createDirectories(newParentDirectoryPath);
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
        RequestPerms requestPerms = new Gson().fromJson(obj, (Type) RequestPerms.class);
        final int userId = requestPerms.user_id;
        final FlooUser u = state.getUser(userId);
        if (u == null) {
            Flog.info("Unknown user for id %s. Not handling request_perms event.", userId);
            return;
        }
        context.dialogPermsRequest(u.username, new RunLater<String>() {
            @Override
            public void run(String action) {
                outbound.setPerms(action, userId, new String[]{"edit_room"});
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
        FlooUser user = state.users.get(userId);
        if (user == null) {
            return;
        }
        state.removeUser(user.user_id);
        context.removeUser(user);
    }

    void _on_delete_buf(JsonObject obj) {
        final DeleteBuf deleteBuf = new Gson().fromJson(obj, (Type) DeleteBuf.class);
        Buf buf = state.bufs.get(deleteBuf.id);
        if (buf == null) {
            Flog.warn(String.format("Tried to delete a buf that doesn't exist: %d", deleteBuf.id));
            return;
        }
        editor.queue(buf, new RunLater<Buf>() {
            @Override
            public void run(Buf buf) {
                buf.cancelTimeout();
                if (state.bufs != null) {
                    state.bufs.remove(deleteBuf.id);
                    state.pathsToIds.remove(buf.path);
                }
                if (!deleteBuf.unlink) {
                    fileRemovedMessageThrottler.statusMessage(String.format("Removed the file, %s, from the workspace.", buf.path));
                    return;
                }
                String absPath = context.absPath(buf.path);
                final IFile fileByPath = context.iFactory.findFileByPath(absPath);

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
        FlooHighlight res = new Gson().fromJson(obj, (Type) FlooHighlight.class);
        state.lastHighlight = res;
        FlooUser user = state.users.get(res.user_id);
        if (user == null) {
            return;
        }
        state.lastUserHighlights.put(user.username, res);
        _on_highlight(res);
    }

    public void _on_highlight(final FlooHighlight flooHighlight) {
        if (state.bufs == null) {
            return;
        }
        final Buf buf = state.bufs.get(flooHighlight.id);
        editor.queue(buf, new RunLater<Buf>() {
            @Override
            public void run(Buf arg) {
                IDoc iDoc = context.iFactory.getDocument(buf.path);
                if (iDoc == null) {
                    return;
                }
                HighlightContext highlight = new HighlightContext();
                highlight.username = state.getUsername(flooHighlight.user_id);
                highlight.gravatar = state.getGravatar(flooHighlight.user_id);
                highlight.following = state.getFollowing() && !flooHighlight.following;

                if (highlight.following && state.followedUsers.size() > 0) {
                    highlight.following = state.followedUsers.contains(highlight.username);
                }
                highlight.path = buf.path;
                highlight.userid = flooHighlight.user_id;
                highlight.force = flooHighlight.summon;
                highlight.ranges = flooHighlight.ranges;
                iDoc.applyHighlight(highlight);
            }
        });
    }

    void _on_saved(JsonObject obj) {
        final Integer id = obj.get("id").getAsInt();
        final Buf buf = this.state.bufs.get(id);
        editor.queue(buf, new RunLater<Buf>() {
            public void run(Buf b) {
                IDoc document = context.iFactory.getDocument(buf.path);
                if (document == null) {
                    return;
                }
                context.setSaving(true);
                document.save();
                context.setSaving(false);
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
                state.pathsToIds.put(buf.path, buf.id);
                buf.write();
                fileAddedMessageThrottler.statusMessage(String.format("Added the file, %s, to the workspace.", buf.path));
            }
        });
    }

    void _on_perms(JsonObject obj) {
        Perms res = new Gson().fromJson(obj, (Type) Perms.class);

        Boolean previousState = state.can("patch");
        if (res.user_id != state.getMyConnectionId()) {
            return;
        }
        HashSet<String> perms = new HashSet<String>(Arrays.asList(res.perms));
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
                context.iFactory.clearReadOnlyState();
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
        context.setupFloobitsWindow();
        context.readThread(new Runnable() {
            @Override
            public void run() {
                try {
                    RoomInfoResponse ri = new Gson().fromJson(obj, (Type) RoomInfoResponse.class);
                    state.handleRoomInfo(ri);
                    context.statusMessage(String.format("You successfully joined %s.",
                            Utils.getLinkHTML(state.url.toString(), state.url.toString())));

                    DotFloo.write(context.colabDir, state.url.toString());
                    if (ri.branchname != null) {
                        String currentLocalBranch = GitUtils.branchName(context.colabDir);
                        if (currentLocalBranch != null) {
                            if (!currentLocalBranch.equals(ri.branchname)) {
                                String msg = String.format(
                                        "Your current HEAD or branch '%s' is not the same as the remote: '%s'. Continue anyway?",
                                        currentLocalBranch, ri.branchname);
                                if (!context.confirmDialog(msg)) {
                                    context.statusMessage("Disconnected because you decided you were in a conflicting branch.");
                                    context.shutdown();
                                }
                            }
                        }
                    }
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
        if (state == null || state.bufs == null) {
            return;
        }
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
