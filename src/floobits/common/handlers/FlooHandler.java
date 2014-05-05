package floobits.common.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import floobits.FlooContext;
import floobits.Listener;
import floobits.common.*;
import floobits.common.protocol.FlooPatch;
import floobits.common.protocol.FlooUser;
import floobits.common.protocol.receive.*;
import floobits.common.protocol.send.*;
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
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;


public class FlooHandler extends BaseHandler {
    class QueuedAction {
        public final Buf buf;
        public RunLater<Buf> runnable;

        QueuedAction(Buf buf, RunLater<Buf> runnable) {
            this.runnable = runnable;
            this.buf = buf;
        }
        public void run() {
            long l = System.currentTimeMillis();
            synchronized (buf) {
                runnable.run(buf);
            }
            long l1 = System.currentTimeMillis();
            Flog.log("Spent %s in ui thread", l1 -l);
        }
    }

    private final Runnable dequeueRunnable;
    public JsonObject lastHighlight;
    private Boolean shouldUpload = false;
    private HashMap<Integer, HashMap<Integer, LinkedList<RangeHighlighter>>> highlights =
            new HashMap<Integer, HashMap<Integer, LinkedList<RangeHighlighter>>>();
    public Boolean stalking = false;
    private HashSet<String> perms = new HashSet<String>();
    private Map<Integer, FlooUser> users = new HashMap<Integer, FlooUser>();
    private HashMap<Integer, Buf> bufs = new HashMap<Integer, Buf>();
    private final HashMap<String, Integer> paths_to_ids = new HashMap<String, Integer>();
    private int connectionId;
    public Listener listener = new Listener(this);
    public boolean readOnly = false;
    // buffer ids are not removed from readOnlyBufferIds
    public HashSet<Integer> readOnlyBufferIds = new HashSet<Integer>();
    public final ConcurrentLinkedQueue<QueuedAction> queue = new ConcurrentLinkedQueue<QueuedAction>();
    public String username = "";

    public FlooHandler (final FlooContext context, FlooUrl flooUrl, boolean shouldUpload) {
        super(context);
        url = flooUrl;
        this.shouldUpload = shouldUpload;
        dequeueRunnable = new Runnable() {
            @Override
            public void run() {
                Flog.log("Doing %s work", queue.size());
                while (true) {
                    // TODO: set a limit here and continue later
                    QueuedAction action = queue.poll();
                    if (action == null) {
                        return;
                    }
                    action.run();
                }
            }
        };
        if (ProjectRootManager.getInstance(context.project).getProjectSdk() == null) {
            Flog.warn("No SDK detected.");
        }
    }

    public String getUsername(int userId) {
        FlooUser user = users.get(userId);
        if (user == null) {
            return "";
        }
        return user.username;
    }

    /**
     * Get a user by their connection id (userId).
     * @param userId
     * @return null or the FlooUser object for the connection id.
     */
    public FlooUser getUser(int userId) {
        return users.get(userId);
    }

    public int getMyConnectionId() {
        return connectionId;
    }


    public void on_connect () {
        queue.clear();
        Settings settings = new Settings(context);
        username = settings.get("username");
        conn.write(new FlooAuth(settings, this.url.owner, this.url.workspace));
        context.statusMessage(String.format("Opened connection to %s.", url.toString()), false);
    }

    public void on_data (String name, JsonObject obj) {
        String method_name = "_on_" + name;
        Method method;
        try {
            method = this.getClass().getDeclaredMethod(method_name, new Class[]{JsonObject.class});
        } catch (NoSuchMethodException e) {
            Flog.warn(String.format("Could not find %s method.\n%s", method_name, e.toString()));
            return;
        }
        Object objects[] = new Object[1];
        objects[0] = obj;
        Flog.debug("Calling %s", method_name);
        try {
            method.invoke(this, objects);
        } catch (Exception e) {
            Flog.warn(String.format("on_data error \n\n%s", Utils.stackToString(e)));
            if (name.equals("room_info")) {
                context.shutdown();
            }
        }
    }

    public void go() {
        Flog.warn("join workspace");
        PersistentJson persistentJson = PersistentJson.getInstance();
        persistentJson.addWorkspace(url, context.colabDir);
        persistentJson.save();
//        conn = new FlooConn(this);
        conn = new Connection(this, context.loopGroup);
        conn.start();
        listener.start();
    }

    public Buf get_buf_by_path(String absPath) {
        String relPath = context.toProjectRelPath(absPath);
        if (relPath == null) {
            return null;
        }
        Integer id = paths_to_ids.get(FilenameUtils.separatorsToUnix(relPath));
        if (id == null) {
            return null;
        }
        return bufs.get(id);
    }

    public void upload(VirtualFile virtualFile) {
        if (readOnly) {
            return;
        }
        if (!virtualFile.isValid()) {
            return;
        }
        String path = virtualFile.getPath();
        Buf b = get_buf_by_path(path);
        if (b != null) {
            Flog.info("Already in workspace: %s", path);
            return;
        }
        send_create_buf(virtualFile);
    }

    private void initialManageConflicts(RoomInfoResponse ri) {
        final LinkedList<Buf> conflicts = new LinkedList<Buf>();
        final LinkedList<Buf> missing = new LinkedList<Buf>();
        final LinkedList<String> conflictedPaths = new LinkedList<String>();
        for (Map.Entry entry : ri.bufs.entrySet()) {
            Integer buf_id = (Integer) entry.getKey();
            RoomInfoBuf b = (RoomInfoBuf) entry.getValue();
            Buf buf = Buf.createBuf(b.path, b.id, Encoding.from(b.encoding), b.md5, context);
            bufs.put(buf_id, buf);
            paths_to_ids.put(b.path, b.id);
            buf.read();
            if (buf.buf == null) {
                if (buf.path.equals("FLOOBITS_README.md") && buf.id == 1) {
                    send_get_buf(buf.id);
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
                            send_get_buf(buf.id);
                        }
                        for (Buf buf : missing) {
                            send_get_buf(buf.id);
                        }
                    }
                }, new Runnable() {
            @Override
            public void run() {
                for (Buf buf : conflicts) {
                    send_set_buf(buf);
                }
                for (Buf buf : missing) {
                    buf.cancelTimeout();
                    conn.write(new DeleteBuf(buf.id, false));
                }
            }
        }, readOnly,
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
            Buf buf = Buf.createBuf(b.path, b.id, Encoding.from(b.encoding), b.md5, context);
            bufs.put(buf_id, buf);
            paths_to_ids.put(b.path, b.id);
            if (!paths.contains(buf.path)) {
                send_delete_buf(buf);
                continue;
            }
            paths.remove(buf.path);
            buf.read();
            if (buf.buf == null) {
                send_get_buf(buf.id);
                continue;
            }
            if (b.md5.equals(buf.md5)) {
                continue;
            }
            send_set_buf(buf);
        }

        LocalFileSystem instance = LocalFileSystem.getInstance();
        for (String path : paths) {
            VirtualFile fileByPath = instance.findFileByPath(context.absPath(path));
            if (fileByPath == null || !fileByPath.isValid()) {
                Flog.warn(String.format("path is no longer a valid virtual file"));
                continue;
            }
            send_create_buf(fileByPath);

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
            listener.flooDisable();
            FileUtils.writeLines(f, strings);
            VirtualFile fileByIoFile = instance.findFileByIoFile(f);
            if (fileByIoFile != null) {
                fileByIoFile.refresh(false, false);
                ignoreTree.addRules(fileByIoFile);
            }
        } catch (IOException e) {
            Flog.warn(e);
        } finally {
            listener.flooEnable();
        }
        shouldUpload = false;
    }
    void _on_room_info(final JsonObject obj) {
        ThreadSafe.read(context, new Runnable() {
            @Override
            public void run() {
                if (bufs == null) {
                    Flog.info("Disconnected, abandon room info handling.");
                    return;
                }
                context.statusMessage(String.format("You successfully joined %s ", url.toString()), false);
                context.chatManager.openChat();
                RoomInfoResponse ri = new Gson().fromJson(obj, (Type) RoomInfoResponse.class);
                isJoined = true;
                users = ri.users;
                context.chatManager.setUsers(users);
                perms = new HashSet<String>(Arrays.asList(ri.perms));
                if (!can("patch")) {
                    readOnly = true;
                    context.statusMessage("You don't have permission to edit files in this workspace.  All documents will be set to read-only.", false);
                }
                connectionId = Integer.parseInt(ri.user_id);
                Flog.info("Got roominfo with userId %d", connectionId);

                DotFloo.write(context.colabDir, url.toString());

                if (shouldUpload) {
                    if (!readOnly) {
                        initialUpload(ri);
                        return;
                    }
                    context.statusMessage("You don't have permission to update remote files.", false);
                }
                initialManageConflicts(ri);
            }
        });
    }

    void send_create_buf(VirtualFile virtualFile) {
        Buf buf = Buf.createBuf(virtualFile, context);
        if (buf == null) {
            return;
        }
        conn.write(new CreateBuf(buf));
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
            }
        });
    }

    void queue(Buf buf, RunLater<Buf> runnable) {
        if (buf == null) {
            Flog.log("Buf is null abandoning adding new queue action.");
            return;
        }
        QueuedAction queuedAction = new QueuedAction(buf, runnable);
        queue.add(queuedAction);
        if (queue.size() > 1) {
            return;
        }
        ThreadSafe.write(context, dequeueRunnable);
    }

    void _on_create_buf(JsonObject obj) {
        Gson gson = new Gson();
        GetBufResponse res = gson.fromJson(obj, (Type) CreateBufResponse.class);
        Buf buf;
        if (res.encoding.equals(Encoding.BASE64.toString())) {
            buf = new BinaryBuf(res.path, res.id, new Base64().decode(res.buf.getBytes()), res.md5, context);
        } else {
            buf = new TextBuf(res.path, res.id, res.buf, res.md5, context);
        }
        queue(buf, new RunLater<Buf>() {
            @Override
            public void run(Buf buf) {
                if (bufs == null) {
                    return;
                }
                bufs.put(buf.id, buf);
                paths_to_ids.put(buf.path, buf.id);
                buf.write();
                context.statusMessage(String.format("Added the file, %s, to the workspace.", buf.path), false);
            }
        });
    }

    void _on_perms(JsonObject obj) {
        Perms res = new Gson().fromJson(obj, (Type) Perms.class);

        Boolean previousState = can("patch");
        if (res.user_id != this.connectionId) {
            return;
        }
        HashSet perms = new HashSet<String>(Arrays.asList(res.perms));
        if (res.action.equals("add")) {
            this.perms.addAll(perms);
        } else if (res.action.equals("set")) {
            this.perms.clear();
            this.perms.addAll(perms);
        } else if (res.action.equals("remove")) {
            this.perms.removeAll(perms);
        }
        readOnly = !can("patch");
        if (can("patch") != previousState) {
            if (can("patch")) {
                Utils.statusMessage("You can now edit this workspace.", context.project);
                clearReadOnlyBufs();
            } else {
                Utils.errorMessage("You can no longer edit this workspace.", context.project);
            }
        }
    }

    void _on_patch(JsonObject obj) {
        final FlooPatch res = new Gson().fromJson(obj, (Type) FlooPatch.class);
        final Buf buf = this.bufs.get(res.id);
        queue(buf, new RunLater<Buf>() {
            @Override
            public void run(Buf b) {
                if (b.buf == null) {
                    Flog.warn("no buffer");
                    send_get_buf(res.id);
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

    Document get_document(final Integer id) {
        if (bufs == null) {
            return null;
        }
        Buf buf = bufs.get(id);
        if (buf == null) {
            Flog.info("Buf %d is not populated yet", id);
            return null;
        }
        if (buf.buf == null) {
            Flog.info("Buf %s is not populated yet", buf.path);
            return null;
        }
        String absPath = context.absPath(buf.path);

        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(absPath);
        if (virtualFile == null || !virtualFile.exists()) {
            Flog.info("no virtual file for %s", buf.path);
            return null;
        }
        Document d = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (d == null) {
            return null;
        }
        return d;
    }

    Editor get_editor_for_document(Document document) {
        Editor[] editors = EditorFactory.getInstance().getEditors(document, context.project);
        for (Editor editor : editors) {
            Flog.warn("is disposed? %s", editor.isDisposed());
        }
        if (editors.length > 0) {
            return editors[0];
        }
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        if (virtualFile == null) {
            return null;
        }
        return EditorFactory.getInstance().createEditor(document, context.project, virtualFile, true);
    }

    void remove_highlight(Integer userId, final Integer bufId, final Document document) {
        HashMap<Integer, LinkedList<RangeHighlighter>> integerRangeHighlighterHashMap = highlights.get(userId);
        if (integerRangeHighlighterHashMap == null) {
            return;
        }
        final LinkedList<RangeHighlighter> rangeHighlighters = integerRangeHighlighterHashMap.get(bufId);
        if (rangeHighlighters == null) {
            return;
        }
        if (document != null) {
            Editor[] editors = EditorFactory.getInstance().getEditors(document, context.project);
            for (Editor editor : editors) {
                if (editor.isDisposed()) {
                    continue;
                }
                MarkupModel markupModel = editor.getMarkupModel();
                RangeHighlighter[] highlights = markupModel.getAllHighlighters();

                for (RangeHighlighter rangeHighlighter: rangeHighlighters) {
                    for (RangeHighlighter markupHighlighter : highlights) {
                        if (rangeHighlighter == markupHighlighter) {
                            markupModel.removeHighlighter(rangeHighlighter);
                        }
                    }
                }
            }
            rangeHighlighters.clear();
            return;
        }

        final Buf buf = this.bufs.get(bufId);
        queue(buf, new RunLater<Buf>() {
            public void run(Buf b) {
                Document document = get_document(bufId);
                if (document == null) {
                    return;
                }
                Editor editor = get_editor_for_document(document);
                if (editor == null) {
                    return;
                }
                MarkupModel markupModel = editor.getMarkupModel();
                for (RangeHighlighter rangeHighlighter : rangeHighlighters) {
                    try {
                        markupModel.removeHighlighter(rangeHighlighter);
                    } catch (AssertionError e) {
                        Flog.info("Assertion error on removeHighlighter");
                    } catch (Exception e) {
                        Flog.info(Utils.stackToString(e));
                    }
                }
                rangeHighlighters.clear();
            }
        });

    }

    public void _on_highlight(JsonObject obj) {
        final FlooHighlight res = new Gson().fromJson(obj, (Type) FlooHighlight.class);
        final ArrayList<ArrayList<Integer>> ranges = res.ranges;
        final Boolean force = (stalking && !res.following) || res.ping || (res.summon == null ? Boolean.FALSE : res.summon);
        lastHighlight = obj;
        final Buf buf = this.bufs.get(res.id);

        RunLater<Buf> runLater = new RunLater<Buf>() {
            @Override
            public void run(Buf arg) {
                Document document = get_document(res.id);
                if (document == null) {
                    return;
                }
                final FileEditorManager manager = FileEditorManager.getInstance(context.project);
                VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
                String username = getUsername(res.user_id);
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
                remove_highlight(res.user_id, res.id, document);

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
                            listener.flooDisable();
                            rangeHighlighter = markupModel.addRangeHighlighter(start, end, HighlighterLayer.ERROR + 100,
                                    attributes, HighlighterTargetArea.EXACT_RANGE);
                        } catch (Exception e) {
                            Flog.warn(e);
                        } finally {
                            listener.flooEnable();
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
                    HashMap<Integer, LinkedList<RangeHighlighter>> integerRangeHighlighterHashMap = highlights.get(res.user_id);

                    if (integerRangeHighlighterHashMap == null) {
                        integerRangeHighlighterHashMap = new HashMap<Integer, LinkedList<RangeHighlighter>>();
                        highlights.put(res.user_id, integerRangeHighlighterHashMap);
                    }
                    integerRangeHighlighterHashMap.put(res.id, rangeHighlighters);
                }
            }
        };
        queue(buf, runLater);

    }

    void _on_saved(JsonObject obj) {
        final Integer id = obj.get("id").getAsInt();
        final Buf buf = this.bufs.get(id);
        queue(buf, new RunLater<Buf>() {
            public void run(Buf b) {
                Document document = get_document(id);
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

    private void set_buf_path(Buf buf, String newPath) {
        paths_to_ids.remove(buf.path);
        buf.path = newPath;
        paths_to_ids.put(buf.path, buf.id);
    }

    void _on_rename_buf(JsonObject jsonObject) {
        final String name = jsonObject.get("old_path").getAsString();
        final String oldPath = context.absPath(name);
        final String newPath = context.absPath(jsonObject.get("path").getAsString());

        Buf buf = get_buf_by_path(oldPath);
        if (buf == null) {
            if (get_buf_by_path(newPath) == null) {
                Flog.warn("Rename oldPath and newPath don't exist. %s %s", oldPath, newPath);
            } else {
                Flog.info("We probably renamed this, nothing to rename.");
            }
            return;
        }

        queue(buf, new RunLater<Buf>() {
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
                set_buf_path(buf, newRelativePath);

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
                    Flog.warn("Error moving file %s %s %s", e,oldPath, newPath);
                }
            }}
        );
    }

    void _on_request_perms(JsonObject obj) {
        Flog.log("got perms receive %s", obj);
        RequestPerms requestPerms = new Gson().fromJson(obj, (Type)RequestPerms.class);
        final int userId = requestPerms.user_id;
        final FlooUser u = users.get(userId);
        if (u == null) {
            Flog.info("Unknown user for id %s. Not handling request_perms event. %d", connectionId);
            return;
        }
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                HandleRequestPermsRequestDialog d = new HandleRequestPermsRequestDialog(u.username, context.project, new RunLater<String>() {
                    @Override
                    public void run(String action) {
                        String[] perms = new String[]{"edit_room"};
                        conn.write(new PermsChange(action, userId, perms));
                    }
                });
                d.createCenterPanel();
                d.show();
            }
        });
    }

    void _on_join(JsonObject obj) {
        FlooUser u = new Gson().fromJson(obj, (Type) FlooUser.class);
        this.users.put(u.user_id, u);
        context.statusMessage(String.format("%s joined the workspace on %s (%s).", u.username, u.platform, u.client), false);
        context.chatManager.setUsers(this.users);
    }

    void _on_part(JsonObject obj) {
        Integer userId = obj.get("user_id").getAsInt();
        FlooUser u = users.get(userId);
        this.users.remove(userId);
        context.chatManager.setUsers(this.users);
        HashMap<Integer, LinkedList<RangeHighlighter>> integerRangeHighlighterHashMap = highlights.get(userId);
        if (integerRangeHighlighterHashMap == null) {
            return;
        }
        for (Entry<Integer, LinkedList<RangeHighlighter>> entry : integerRangeHighlighterHashMap.entrySet()) {
            remove_highlight(userId, entry.getKey(), null);
        }
        context.statusMessage(String.format("%s left the workspace.", u.username), false);
    }

    void _on_error(JsonObject jsonObject) {
        isJoined = false;
        String reason = jsonObject.get("msg").getAsString();
        reason = String.format("Floobits Error: %s", reason);
        Flog.warn(reason);
        if (jsonObject.has("flash") && jsonObject.get("flash").getAsBoolean()) {
            context.errorMessage(reason);
            context.flashMessage(reason);
        }
    }

    void _on_disconnect(JsonObject jsonObject) {
        isJoined = false;
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
        Buf buf = bufs.get(deleteBuf.id);
        if (buf == null) {
            Flog.warn(String.format("Tried to delete a buf that doesn't exist: %s", deleteBuf.id));
            return;
        }
        queue(buf, new RunLater<Buf>() {
            @Override
            public void run(Buf buf) {
                buf.cancelTimeout();
                if (bufs != null) {
                    bufs.remove(deleteBuf.id);
                    paths_to_ids.remove(buf.path);
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
        conn.write(new Pong());
    }

    public void send_get_buf (Integer buf_id) {
        Buf buf = bufs.get(buf_id);
        if (buf == null) {
            return;
        }
        synchronized (buf) {
            buf.set(null, null);
        }
        conn.write(new GetBuf(buf_id));
    }

    public void send_patch (String textPatch, String before_md5, TextBuf buf) {
        if (!can("patch")) {
            return;
        }
        Flog.log("Sending patch for %s", buf.path);
        FlooPatch req = new FlooPatch(textPatch, before_md5, buf);
        conn.write(req);
    }

    public void send_delete_buf(Buf buf) {
        buf.cancelTimeout();
        conn.write(new DeleteBuf(buf.id, false));
    }

    public void send_set_buf (Buf b) {
        if (!can("patch")) {
            return;
        }
        conn.write(new SetBuf(b));
    }

    public void send_summon (String current, Integer offset) {
        if (!can("patch")) {
            return;
        }
        Buf buf = this.get_buf_by_path(current);
        if (Buf.isBad(buf)) {
            context.errorMessage(String.format("The file %s is not shared!", current));
            return;
        }
        ArrayList<ArrayList<Integer>> ranges = new ArrayList<ArrayList<Integer>>();
        ranges.add(new ArrayList<Integer>(Arrays.asList(offset, offset)));
        conn.write(new FlooHighlight(buf, ranges, true, stalking));
    }

    public void untellij_renamed(String path, String newPath) {
        if (!can("patch")) {
            return;
        }
        Flog.log("Renamed buf: %s - %s", path, newPath);
        Buf buf = this.get_buf_by_path(path);
        if (buf == null) {
            Flog.info("buf does not exist.");
            return;
        }
        if (!perms.contains("patch")) {
            Flog.info("we cant patch because perms");
            return;
        }
        String newRelativePath = context.toProjectRelPath(newPath);
        if (newRelativePath == null) {
            Flog.warn(String.format("%s was moved to %s, deleting from workspace.", buf.path, newPath));
            buf.cancelTimeout();
            this.conn.write(new DeleteBuf(buf.id, true));
            return;
        }
        if (buf.path.equals(newRelativePath)) {
            Flog.info("untellij_renamed handling workspace rename, aborting.");
            return;
        }
        buf.cancelTimeout();
        conn.write(new RenameBuf(buf.id, newRelativePath));
        set_buf_path(buf, newRelativePath);
    }

    public void untellij_changed(VirtualFile file) {
        String filePath = file.getPath();
        if (!can("patch")) {
            return;
        }
        if (!context.isShared(filePath)) {
            return;
        }
        final Buf buf = this.get_buf_by_path(filePath);
        if (buf == null) {
            return;
        }
        synchronized (buf) {
            if (Buf.isBad(buf)) {
                Flog.info("buf isn't populated yet %s", file.getPath());
                return;
            }
            buf.send_patch(file);
        }
    }

    public void untellij_selection_change(String path, ArrayList<ArrayList<Integer>> textRanges) {
        Buf buf = this.get_buf_by_path(path);
        if (!can("highlight")) {
            return;
        }
        if (Buf.isBad(buf)) {
            Flog.info("buf isn't populated yet %s", path);
            return;
        }
        conn.write(new FlooHighlight(buf, textRanges, false, stalking));
    }

    public void untellij_saved(String path) {
        Buf buf = this.get_buf_by_path(path);

        if (Buf.isBad(buf)) {
            Flog.info("buf isn't populated yet %s", path);
            return;
        }
        if (!can("patch")) {
            return;
        }
        conn.write(new SaveBuf(buf.id));
    }

    public void untellij_soft_delete(HashSet<String> files) {
        if (!can("patch")) {
            return;
        }

        for (String path : files) {
            Buf buf = get_buf_by_path(path);
            if (buf == null) {
                context.statusMessage(String.format("The file, %s, is not in the workspace.", path), NotificationType.WARNING);
                continue;
            }
            buf.cancelTimeout();
            conn.write(new DeleteBuf(buf.id, false));
        }
    }

    void untellij_deleted(String path) {
        Buf buf = this.get_buf_by_path(path);
        if (buf == null) {
            Flog.info("buf does not exist");
            return;
        }
        if (!can("patch")) {
            return;
        }
        buf.cancelTimeout();

        conn.write(new DeleteBuf(buf.id, true));
    }

    public void untellij_deleted_directory(ArrayList<String> filePaths) {
        if (!can("patch")) {
            return;
        }

        for (String filePath : filePaths) {
            untellij_deleted(filePath);
        }
    }

    public void untellij_msg(String chatContents) {
        conn.write(new FlooMessage(chatContents));
    }

    public void untellij_kick(int userId) {
        if (!can("kick")) {
            return;
        }
        conn.write(new FlooKick(userId));
    }

    public void untellij_perms_change(int userId, String[] perms) {
        if (!can("kick")) {
            return;
        }
        conn.write(new PermsChange("set", userId, perms));
        changePermsForUser(userId, perms);
    }

    public void changePermsForUser(int userId, String[] permissions) {
        FlooUser user = getUser(userId);
        if (user == null) {
            return;
        }
        List<String> givenPerms = java.util.Arrays.asList(permissions);
        Set<String> translatedPermsSet = new HashSet<String>();
        HashMap<String, String[]> permTypes = new HashMap<String, String[]>();
        permTypes.put("edit_room", new String[]{
            "patch", "get_buf", "set_buf", "create_buf", "delete_buf", "rename_buf", "set_temp_data", "delete_temp_data",
            "highlight", "msg", "datamsg", "create_term", "term_stdin", "delete_term", "update_term", "term_stdout", "saved"
        });
        permTypes.put("view_room", new String[]{"get_buf", "ping", "pong"});
        permTypes.put("request_perms", new String[]{"get_buf", "request_perms"});
        permTypes.put("admin_room", new String[]{"kick", "pull_repo", "perms"});
        for (Map.Entry<String, String[]> entry : permTypes.entrySet()) {
            if (givenPerms.contains(entry.getKey())) {
                for (String perm : entry.getValue()) {
                    translatedPermsSet.add(perm);
                }
            }
        }
        user.perms = translatedPermsSet.toArray(new String[translatedPermsSet.size()]);
    }

    public boolean can(String perm) {
        if (!isJoined)
            return false;

        if (!perms.contains(perm)) {
            Flog.info("we can't do that because perms");
            return false;
        }
        return true;
    }

    protected void clearReadOnlyBufs() {
        for (Integer bufferId : readOnlyBufferIds) {
            Buf buf = bufs.get(bufferId);
            if (buf == null) {
                continue;
            }
            buf.clearReadOnly();
        }
        readOnlyBufferIds.clear();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        listener.stop();
        clearReadOnlyBufs();
        clearHighlights();
        highlights = null;
        bufs = null;
        queue.clear();
        context.statusMessage(String.format("Leaving workspace: %s.", url.toString()), false);
        context.chatManager.clearUsers();
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

    public void clearHighlights() {
        if (highlights == null || highlights.size() <= 0) {
            return;
        }
        for (Entry<Integer, HashMap<Integer, LinkedList<RangeHighlighter>>> entry : highlights.entrySet()) {
            HashMap<Integer, LinkedList<RangeHighlighter>> highlightsForUser = entry.getValue();
            if (highlightsForUser == null || highlightsForUser.size() <= 0) {
                continue;
            }
            Integer user_id = entry.getKey();
            for (Entry<Integer, LinkedList<RangeHighlighter>> integerLinkedListEntry: highlightsForUser.entrySet()) {
                remove_highlight(user_id, integerLinkedListEntry.getKey(), null);
            }
        }
    }

    public void sendEditRequest() {
        if (!can("request_perms")) {
            Utils.errorMessage("You are not allowed to ask for edit permissions.", context.project);
            return;
        }
        conn.write(new EditRequest(new ArrayList<String>(Arrays.asList("edit_room"))));
    }

}
