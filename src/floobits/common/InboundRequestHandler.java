package floobits.common;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import floobits.FlooContext;
import floobits.Listener;
import floobits.common.protocol.FlooPatch;
import floobits.common.protocol.FlooUser;
import floobits.common.protocol.receive.*;
import floobits.common.protocol.send.CreateBufResponse;
import floobits.common.protocol.send.RoomInfoResponse;
import floobits.dialogs.DisconnectNoticeDialog;
import floobits.dialogs.HandleRequestPermsRequestDialog;
import floobits.dialogs.HandleTooBigDialog;
import floobits.dialogs.ResolveConflictsDialog;
import floobits.utilities.Colors;
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

/**
 * Created by kans on 5/7/14.
 */
public class InboundRequestHandler {
    private FlooContext context;
    private final FloobitsState state;
    private EditorManager editor;
    private final OutboundRequestHandler outbound;
    private boolean shouldUpload;

    public InboundRequestHandler(FlooContext context, FloobitsState state, EditorManager editor, OutboundRequestHandler outbound, boolean shouldUpload) {
        this.context = context;
        this.state = state;
        this.editor = editor;
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
        context.statusMessage("Stomping on remote files and uploading new ones.", false);
        context.flashMessage("Stomping on remote files and uploading new ones.");

        final Ignore ignoreTree = context.getIgnoreTree();
        ArrayList<Ignore> allIgnores = new ArrayList<Ignore>();
        LinkedList<Ignore> tempIgnores = new LinkedList<Ignore>(){{ add(ignoreTree);}};
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
            for (VirtualFile virtualFile : ig.files)
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

        LocalFileSystem instance = LocalFileSystem.getInstance();
        for (String path : paths) {
            VirtualFile fileByPath = instance.findFileByPath(context.absPath(path));
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
            VirtualFile fileByIoFile = instance.findFileByIoFile(f);
            if (fileByIoFile != null) {
                fileByIoFile.refresh(false, false);
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
                Flog.info("We probably renamed this, nothing to rename.");
            }
            return;
        }

        editor.queue(buf, new RunLater<Buf>() {
                    @Override
                    public void run(Buf buf) {
                        final VirtualFile foundFile = LocalFileSystem.getInstance().findFileByPath(oldPath);
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
                        try {
                            foundFile.rename(null, newFileName);
                        } catch (IOException e) {
                            Flog.warn("Error renaming file %s %s %s", e, oldPath, newPath);
                        }
                        // Move file
                        String newParentDirectoryPath = newFile.getParent();
                        String oldParentDirectoryPath = oldFile.getParent();
                        if (newParentDirectoryPath.equals(oldParentDirectoryPath)) {
                            Flog.warn("Only renamed file, don't need to move %s %s", oldPath, newPath);
                            return;
                        }
                        VirtualFile directory = null;
                        try {
                            directory = VfsUtil.createDirectories(newParentDirectoryPath);
                        } catch (IOException e) {
                            Flog.warn("Failed to create directories in time for moving file. %s %s", oldPath, newPath);

                        }
                        if (directory == null) {
                            Flog.warn("Failed to create directories in time for moving file. %s %s", oldPath, newPath);
                            return;
                        }
                        try {
                            foundFile.move(null, directory);
                        } catch (IOException e) {
                            Flog.warn("Error moving file %s %s %s", e, oldPath, newPath);
                        }
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
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                HandleRequestPermsRequestDialog d = new HandleRequestPermsRequestDialog(u.username, context.project, new RunLater<String>() {
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
        Integer userId = obj.get("user_id").getAsInt();
        state.removeUser(userId);
        HashMap<Integer, LinkedList<RangeHighlighter>> integerRangeHighlighterHashMap = state.highlights.get(userId);
        if (integerRangeHighlighterHashMap == null) {
            return;
        }
        for (Map.Entry<Integer, LinkedList<RangeHighlighter>> entry : integerRangeHighlighterHashMap.entrySet()) {
            editor.remove_highlight(userId, entry.getKey(), null);
        }
    }

    void _on_error(JsonObject jsonObject) {
        context.disconnected();
        String reason = jsonObject.get("msg").getAsString();
        reason = String.format("Floobits Error: %s", reason);
        Flog.warn(reason);
        if (jsonObject.has("flash") && jsonObject.get("flash").getAsBoolean()) {
            context.errorMessage(reason);
            context.flashMessage(reason);
        }
    }

    void _on_disconnect(JsonObject jsonObject) {
        context.disconnected();
        String reason = jsonObject.get("reason").getAsString();
        if (reason != null) {
            context.errorMessage(String.format("You have been disconnected from the workspace because %s", reason));
            context.flashMessage("You have been disconnected from the workspace.");
        } else {
            context.statusMessage("You have left the workspace", false);
        }
        context.shutdown();
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
                    context.statusMessage(String.format("Removed the file, %s, from the workspace.", buf.path), false);
                    return;
                }
                String absPath = context.absPath(buf.path);
                final VirtualFile fileByPath = LocalFileSystem.getInstance().findFileByPath(absPath);

                if (fileByPath == null) {
                    return;
                }
                try {
                    fileByPath.delete(this);
                } catch (IOException e) {
                    Flog.warn(e);
                }
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
        if (!context.chatManager.isOpen()) {
            context.statusMessage(String.format("%s: %s", username, msg), true);
        }
        context.chatManager.chatMessage(username, msg, messageDate);
    }

    void _on_term_stdout(JsonObject jsonObject) {}
    void _on_term_stdin(JsonObject jsonObject) {}

    void _on_ping(JsonObject jsonObject) {
        outbound.pong();
    }

    public void _on_highlight(JsonObject obj) {
        final FlooHighlight res = new Gson().fromJson(obj, (Type) FlooHighlight.class);
        final ArrayList<ArrayList<Integer>> ranges = res.ranges;
        final Boolean force = (state.stalking && !res.following) || res.ping || (res.summon == null ? Boolean.FALSE : res.summon);
        state.lastHighlight = obj;
        final Buf buf = this.state.bufs.get(res.id);

        RunLater<Buf> runLater = new RunLater<Buf>() {
            @Override
            public void run(Buf arg) {
                Document document = editor.get_document(res.id);
                if (document == null) {
                    return;
                }
                final FileEditorManager manager = FileEditorManager.getInstance(context.project);
                VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
                String username = state.getUsername(res.user_id);
                if (virtualFile != null) {
                    Boolean summon = false;
                    if (res.summon != null) {
                        summon = res.summon;
                    }
                    if ((res.ping || summon) && username != null) {
                        context.statusMessage(String.format("%s has summoned you to %s", username, virtualFile.getPath()), false);
                    }
                    if (force && virtualFile.isValid()) {
                        manager.openFile(virtualFile, true, true);
                    }
                }
                editor.remove_highlight(res.user_id, res.id, document);

                int textLength = document.getTextLength();
                if (textLength == 0) {
                    return;
                }
                TextAttributes attributes = new TextAttributes();
                JBColor color = Colors.getColorForUser(username);
                attributes.setEffectColor(color);
                attributes.setEffectType(EffectType.SEARCH_MATCH);
                attributes.setBackgroundColor(color);
                attributes.setForegroundColor(Colors.getFGColor());

                boolean first = true;
                Editor[] editors = EditorFactory.getInstance().getEditors(document, context.project);

                for (Editor editor : editors) {
                    if (editor.isDisposed()) {
                        continue;
                    }
                    MarkupModel markupModel = editor.getMarkupModel();
                    LinkedList<RangeHighlighter> rangeHighlighters = new LinkedList<RangeHighlighter>();
                    for (List<Integer> range : ranges) {
                        int start = range.get(0);
                        int end = range.get(1);
                        if (start == end) {
                            end += 1;
                        }
                        if (end > textLength) {
                            end = textLength;
                        }
                        if (start >= textLength) {
                            start = textLength - 1;
                        }
                        RangeHighlighter rangeHighlighter = null;
                        try {
                            Listener.flooDisable();
                            rangeHighlighter = markupModel.addRangeHighlighter(start, end, HighlighterLayer.ERROR + 100,
                                    attributes, HighlighterTargetArea.EXACT_RANGE);
                        } catch (Exception e) {
                            Flog.warn(e);
                        } finally {
                            Listener.flooEnable();
                        }
                        if (rangeHighlighter == null) {
                            continue;
                        }
                        rangeHighlighters.add(rangeHighlighter);
                        if (force && first) {
                            CaretModel caretModel = editor.getCaretModel();
                            caretModel.moveToOffset(start);
                            LogicalPosition position = caretModel.getLogicalPosition();
                            ScrollingModel scrollingModel = editor.getScrollingModel();
                            scrollingModel.scrollTo(position, ScrollType.MAKE_VISIBLE);
                            first = false;
                        }
                    }
                    HashMap<Integer, LinkedList<RangeHighlighter>> integerRangeHighlighterHashMap = state.highlights.get(res.user_id);

                    if (integerRangeHighlighterHashMap == null) {
                        integerRangeHighlighterHashMap = new HashMap<Integer, LinkedList<RangeHighlighter>>();
                        state.highlights.put(res.user_id, integerRangeHighlighterHashMap);
                    }
                    integerRangeHighlighterHashMap.put(res.id, rangeHighlighters);
                }
            }
        };
        editor.queue(buf, runLater);

    }

    void _on_saved(JsonObject obj) {
        final Integer id = obj.get("id").getAsInt();
        final Buf buf = this.state.bufs.get(id);
        editor.queue(buf, new RunLater<Buf>() {
            public void run(Buf b) {
                Document document = editor.get_document(id);
                if (document == null) {
                    return;
                }
                if (!ReadonlyStatusHandler.ensureDocumentWritable(context.project, document)) {
                    Flog.info("Document: %s is not writable, can not save.", document);
                    return;
                }
                FileDocumentManager.getInstance().saveDocument(document);
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
                context.statusMessage(String.format("Added the file, %s, to the workspace.", buf.path), false);
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
                Utils.statusMessage("You state.can now edit this workspace.", context.project);
                editor.clearReadOnlyState();
            } else {
                Utils.errorMessage("You state.can no longer edit this workspace.", context.project);
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
                if (state.bufs == null) {
                    Flog.info("Disconnected, abandon room info handling.");
                    return;
                }
                context.statusMessage(String.format("You successfully joined %s ", state.url.toString()), false);
                context.chatManager.openChat();
                RoomInfoResponse ri = new Gson().fromJson(obj, (Type) RoomInfoResponse.class);
                state.handleRoomInfo(ri);

                DotFloo.write(context.colabDir, state.url.toString());
                if (shouldUpload) {
                    if (!state.readOnly) {
                        initialUpload(ri);
                        return;
                    }
                    context.statusMessage("You don't have permission to update remote files.", false);
                }
                initialManageConflicts(ri);
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

    @SuppressWarnings("unused")
    public void testHandlers () throws IOException {
        JsonObject obj = new JsonObject();
        _on_room_info(obj);
        _on_get_buf(obj);
        _on_patch(obj);
        _on_highlight(obj);
        _on_saved(obj);
        _on_join(obj);
        _on_part(obj);
        _on_disconnect(obj);
        _on_create_buf(obj);
        _on_request_perms(obj);
        _on_msg(obj);
        _on_rename_buf(obj);
        _on_term_stdin(obj);
        _on_term_stdout(obj);
        _on_delete_buf(obj);
        _on_perms(obj);
        _on_error(obj);
        _on_ping(obj);
    }
}
