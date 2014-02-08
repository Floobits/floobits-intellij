package floobits.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import floobits.*;
import floobits.common.*;
import floobits.common.protocol.*;
import floobits.common.protocol.receive.*;
import floobits.common.protocol.send.*;
import floobits.dialogs.ResolveConflictsDialogWrapper;
import floobits.utilities.Colors;
import floobits.utilities.Flog;
import floobits.utilities.ThreadSafe;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;

abstract class DocumentFetcher {
    Boolean make_document = false;

    DocumentFetcher(Boolean make_document) {
        this.make_document = make_document;
    }

    abstract public void on_document(Document document);

    public void fetch(final FlooContext context, final String path) {
        ThreadSafe.write(context, new Runnable() {
            public void run() {
                String absPath = context.absPath(path);

                VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(absPath);
                if (virtualFile == null || !virtualFile.exists()) {
                    Flog.info("no virtual file for %s", path);
                    return;
                }
                Document d = FileDocumentManager.getInstance().getDocument(virtualFile);
                if (d == null) {
                    return;
                }
                on_document(d);
            }
        });
    }
}

public class FlooHandler extends BaseHandler {
    private Boolean shouldUpload = false;
    private final HashMap<Integer, HashMap<Integer, LinkedList<RangeHighlighter>>> highlights =
            new HashMap<Integer, HashMap<Integer, LinkedList<RangeHighlighter>>>();
    public Boolean stalking = false;
    private HashSet<String> perms = new HashSet<String>();
    private Map<Integer, FlooUser> users = new HashMap<Integer, FlooUser>();
    private final HashMap<Integer, Buf> bufs = new HashMap<Integer, Buf>();
    private final HashMap<String, Integer> paths_to_ids = new HashMap<String, Integer>();
    private RoomInfoTree tree;
    private FlooConn conn;
    private String user_id;
    public Listener listener = new Listener(this);
    public boolean readOnly = false;


    String get_username(Integer user_id) {
        FlooUser user = users.get(user_id);
        if (user == null) {
            return "";
        }
        return user.username;
    }

    public void on_connect () {
        this.conn.write(new FlooAuth(new Settings(context), this.url.owner, this.url.workspace));
        context.status_message(String.format("You successfully joined %s ", url.toString()));
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
            Flog.warn("on_data error %s", e);
            if (name.equals("room_info")) {
                shutDown();
            }
        }
    }

    public FlooHandler (FlooContext context, FlooUrl flooUrl, boolean shouldUpload) {
        super(context);
        url = flooUrl;
        this.shouldUpload = shouldUpload;
    }

    public void go() {
        Flog.warn("join workspace");
        PersistentJson persistentJson = PersistentJson.getInstance();
        persistentJson.addWorkspace(url, context.colabDir);
        persistentJson.save();
        conn = new FlooConn(this);
        conn.start();
        listener.start();
    }

    Buf get_buf_by_path(String absPath) {
        String relPath = context.toProjectRelPath(absPath);
        if (relPath == null) {
            return null;
        }
        Integer id = this.paths_to_ids.get(FilenameUtils.separatorsToUnix(relPath));
        if (id == null) {
            return null;
        }
        return this.bufs.get(id);
    }

    void upload() {
        ProjectRootManager.getInstance(context.project).getFileIndex().iterateContent(new ContentIterator() {
            public boolean processFile(final VirtualFile virtualFile) {
                if (!context.isIgnored(virtualFile)) upload(virtualFile);
                return true;
            }
        });
    }

    public void upload(VirtualFile virtualFile) {
        String path = context.toProjectRelPath(virtualFile.getPath());

        Buf b = get_buf_by_path(path);
        if (b != null) {
            Flog.info("Already in workspace: %s", path);
            return;
        }
        send_create_buf(virtualFile);
    }

    void _on_room_info(JsonObject obj) {
        RoomInfoResponse ri = new Gson().fromJson(obj, (Type) RoomInfoResponse.class);
        isJoined = true;
        this.tree = new RoomInfoTree(obj.getAsJsonObject("tree"));
        this.users = ri.users;
        this.perms = new HashSet<String>(Arrays.asList(ri.perms));
        if (!can("patch")){
            readOnly = true;
            context.status_message("You don't have permission to edit files in this workspace.  All documents will be set to read-only.");
        }
        this.user_id = ri.user_id;

        DotFloo.write(context.colabDir, url.toString());

        final LinkedList<Buf> conflicts = new LinkedList<Buf>();
        final LinkedList<String> conflictedPaths = new LinkedList<String>();
        for (Map.Entry entry : ri.bufs.entrySet()) {
            Integer buf_id = (Integer) entry.getKey();
            RoomInfoBuf b = (RoomInfoBuf) entry.getValue();
            Buf buf = Buf.createBuf(b.path, b.id, Encoding.from(b.encoding), b.md5, context);
            this.bufs.put(buf_id, buf);
            this.paths_to_ids.put(b.path, b.id);
            buf.read();
            if (buf.buf == null) {
                this.send_get_buf(buf.id);
                continue;
            }
            if (!b.md5.equals(buf.md5)) {
                conflicts.add(buf);
                conflictedPaths.add(buf.path);
            }
        }

        if (shouldUpload) {
            if (readOnly) {
                context.status_message("You don't have permission to update remote files.");
            } else {
                context.status_message("Stomping on remote files and uploading new ones.");
                context.flash_message("Stomping on remote files and uploading new ones.");
                upload();
                for (Buf buf : conflicts) {
                    send_set_buf(buf);
                }
                return;
            }
        }

        if (conflicts.size() <= 0) {
           return;
        }

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                String[] conflictedPathsArray = conflictedPaths.toArray(new String[conflictedPaths.size()]);
                ResolveConflictsDialogWrapper dialog = new ResolveConflictsDialogWrapper(
                    new RunLater<Void>() {
                        @Override
                        public void run(Void _) {
                            for (Buf buf : conflicts) {
                                send_get_buf(buf.id);
                            }
                        }
                    }, new RunLater<Void>() {
                        @Override
                        public void run(Void _) {
                            for (Buf buf : conflicts) {
                                send_set_buf(buf);
                            }
                        }
                    }, readOnly,
                     new RunLater<Void>() {
                        @Override
                        public void run(Void _) {
                            shutDown();
                        }
                    }, conflictedPathsArray);
                dialog.createCenterPanel();
                dialog.show();
            }
        });
    }

    void send_create_buf(VirtualFile virtualFile) {
        Buf buf = Buf.createBuf(virtualFile, context);
        if (buf == null) {
            return;
        }
        this.conn.write(new CreateBuf(buf));
    }

    void _on_get_buf(JsonObject obj) {
        // TODO: be nice about this and update the existing view
        Gson gson = new Gson();
        GetBufResponse res = gson.fromJson(obj, (Type) GetBufResponse.class);

        Buf b = this.bufs.get(res.id);
        b.set(res.buf, res.md5);
        b.write();
        Flog.info("on get buffed. %s", b.path);
    }

    void _on_create_buf(JsonObject obj) {
        // TODO: be nice about this and update the existing view
        Gson gson = new Gson();
        GetBufResponse res = gson.fromJson(obj, (Type) CreateBufResponse.class);
        Buf buf;
        if (res.encoding.equals(Encoding.BASE64.toString())) {
            buf = new BinaryBuf(res.path, res.id, new Base64().decode(res.buf.getBytes()), res.md5, context);
        } else {
            buf = new TextBuf(res.path, res.id, res.buf, res.md5, context);
        }
        this.bufs.put(buf.id, buf);
        this.paths_to_ids.put(buf.path, buf.id);
        buf.write();
    }

    void _on_perms(JsonObject obj) {
        Perms res = new Gson().fromJson(obj, (Type) Perms.class);

        if (!res.user_id.equals(this.user_id)) {
            return;
        }
        HashSet perms = new HashSet<String>(Arrays.asList(res.perms));
        if (res.action.equals("add")) {
            this.perms.addAll(perms);
        } else if (res.action.equals("remove")) {
            this.perms.removeAll(perms);
        }
        readOnly = !can("patch");
    }

    void _on_patch(JsonObject obj) {
        final FlooPatch res = new Gson().fromJson(obj, (Type) FlooPatch.class);
        final Buf b = this.bufs.get(res.id);
        if (b.buf == null) {
            Flog.warn("no buffer");
            this.send_get_buf(res.id);
            return;
        }

        if (res.patch.length() == 0) {
            Flog.warn("wtf? no patches to apply. server is being stupid");
            return;
        }
        b.patch(res);
    }

    void get_document(Integer id, DocumentFetcher documentFetcher) {
        Buf buf = this.bufs.get(id);
        if (buf == null) {
            Flog.info("Buf %d is not populated yet", id);
            return;
        }
        if (buf.buf == null) {
            Flog.info("Buf %s is not populated yet", buf.path);
            return;
        }

        this.get_document(buf.path, documentFetcher);
    }

    void get_document(String path, DocumentFetcher documentFetcher) {
        documentFetcher.fetch(context, path);
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

    void remove_highlight(Integer userId, Integer bufId, Document document) {
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

        get_document(bufId, new DocumentFetcher(false) {
            @Override
            public void on_document(Document document) {
                Editor editor = get_editor_for_document(document);
                MarkupModel markupModel = editor.getMarkupModel();

                for (RangeHighlighter rangeHighlighter : rangeHighlighters) {
                    markupModel.removeHighlighter(rangeHighlighter);
                }
                rangeHighlighters.clear();
            }
        });

    }

    void _on_highlight(JsonObject obj) {
        final FlooHighlight res = new Gson().fromJson(obj, (Type)FlooHighlight.class);
        final ArrayList<ArrayList<Integer>> ranges = res.ranges;
        final Boolean force = stalking || res.ping || (res.summon == null ? Boolean.FALSE : res.summon);
        get_document(res.id, new DocumentFetcher(force) {
            @Override
            public void on_document(Document document) {
                final FileEditorManager manager = FileEditorManager.getInstance(context.project);
                VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
                String username = get_username(res.user_id);
                if (virtualFile != null) {
                    if ((res.ping || res.summon) && username != null) {
                        context.status_message(String.format("%s has summoned you to %s", username, virtualFile.getPath()));
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
        });
    }

    void _on_saved(JsonObject obj) {
        Integer id = obj.get("id").getAsInt();
        this.get_document(id, new DocumentFetcher(false) {
            @Override
            public void on_document(Document document) {
                FileDocumentManager.getInstance().saveDocument(document);
            }
        });
    }

    private void set_buf_path(Buf buf, String newPath) {
        paths_to_ids.remove(buf.path);
        buf.path = newPath;
        this.paths_to_ids.put(buf.path, buf.id);
    }

    void _on_rename_buf(JsonObject jsonObject) {
        final String name = jsonObject.get("old_path").getAsString();
        final String oldPath = context.absPath(name);
        final String newPath = context.absPath(jsonObject.get("path").getAsString());
        final VirtualFile foundFile = LocalFileSystem.getInstance().findFileByPath(oldPath);
        Buf buf = get_buf_by_path(oldPath);
        if (buf == null) {
            if (get_buf_by_path(newPath) == null) {
                Flog.warn("Rename oldPath and newPath don't exist. %s %s", oldPath, newPath);
            } else {
                Flog.info("We probably renamed this, nothing to rename.");
            }
            return;
        }
        if (foundFile == null) {
            Flog.warn("File we want to move was not found %s %s.", oldPath, newPath);
            return;
        }
        String newRelativePath = context.toProjectRelPath(newPath);
        set_buf_path(buf, newRelativePath);
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    public void run() {
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
                    }
                });
            }
        });
    }

    void _on_request_perms(JsonObject obj) {
        Flog.log("got perms receive");
    }

    void _on_join(JsonObject obj) {
        FlooUser u = new Gson().fromJson(obj, (Type)FlooUser.class);
        this.users.put(u.user_id, u);
        context.status_message(String.format("%s joined the workspace on %s (%s).", u.username, u.platform, u.client));
    }

    void _on_part(JsonObject obj) {
        Integer userId = obj.get("user_id").getAsInt();
        FlooUser u = users.get(userId);
        this.users.remove(userId);
        HashMap<Integer, LinkedList<RangeHighlighter>> integerRangeHighlighterHashMap = highlights.get(userId);
        if (integerRangeHighlighterHashMap == null) {
            return;
        }
        for (Entry<Integer, LinkedList<RangeHighlighter>> entry : integerRangeHighlighterHashMap.entrySet()) {
            remove_highlight(userId, entry.getKey(), null);
        }
        context.status_message(String.format("%s left the workspace.", u.username));

    }

    void _on_disconnect(JsonObject jsonObject) {
        isJoined = false;
        String reason = jsonObject.get("reason").getAsString();
        if (reason != null) {
            context.error_message(reason);
            context.flash_message(reason);
        } else {
            context.status_message("You have left the workspace");
        }
        shutDown();
    }

    void _on_delete_buf(JsonObject jsonObject) {
        Integer id = jsonObject.get("id").getAsInt();
        Buf buf = bufs.get(id);
        if (buf == null) {
            Flog.warn(String.format("Tried to delete a buf that doesn't exist: %s", id));
            return;
        }
        String absPath = context.absPath(buf.path);
        buf.cancelTimeout();
        bufs.remove(id);
        paths_to_ids.remove(buf.path);
        final VirtualFile fileByPath = LocalFileSystem.getInstance().findFileByPath(absPath);
        if (fileByPath == null) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    public void run() {
                        try {
                            fileByPath.delete(this);
                        } catch (IOException e) {
                            Flog.warn(e);
                        }
                    }
                });
            }
        });
    }

    void _on_msg(JsonObject jsonObject){
        String msg = jsonObject.get("data").getAsString();
        String username = jsonObject.get("username").getAsString();
        context.status_message(String.format("%s: %s", username, msg));
    }

    void _on_term_stdout(JsonObject jsonObject) {}
    void _on_term_stdin(JsonObject jsonObject) {}

    public void send_get_buf (Integer buf_id) {
        Buf buf = bufs.get(buf_id);
        if (buf != null) {
            buf.set(null, null);
        }
        this.conn.write(new GetBuf(buf_id));
    }

    public void send_patch (String textPatch, String before_md5, TextBuf buf) {
        if (!can("patch")) {
            return;
        }
        Flog.log("Patching %s", buf.path);
        FlooPatch req = new FlooPatch(textPatch, before_md5, buf);
        this.conn.write(req);
    }

    public void send_set_buf (Buf b) {
        if (!can("patch")) {
            return;
        }
        this.conn.write(new SetBuf(b));
    }

    public void send_summon (String current, Integer offset) {
        if (!can("patch")) {
            return;
        }
        Buf buf = this.get_buf_by_path(current);
        if (Buf.isBad(buf)) {
            return;
        }
        ArrayList<ArrayList<Integer>> ranges = new ArrayList<ArrayList<Integer>>();
        ranges.add(new ArrayList<Integer>(Arrays.asList(offset, offset)));
        this.conn.write(new FlooHighlight(buf, ranges, true));
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
        if (buf.path.equals(newRelativePath)) {
            Flog.info("untellij_renamed handling workspace rename, aborting.");
            return;
        }
        buf.cancelTimeout();
        this.conn.write(new RenameBuf(buf.id, newRelativePath));
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
        Buf buf = this.get_buf_by_path(filePath);

        if (Buf.isBad(buf)) {
            Flog.info("buf isn't populated yet %s", file.getPath());
            return;
        }
        buf.send_patch(file);
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
        this.conn.write(new FlooHighlight(buf, textRanges, false));
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
        this.conn.write(new SaveBuf(buf.id));
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
        this.conn.write(new DeleteBuf(buf.id));
    }

    public void untellij_deleted_directory(ArrayList<String> filePaths) {
        if (!can("patch")) {
            return;
        }

        for (String filePath : filePaths) {
            untellij_deleted(filePath);
        }
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

    @Override
    public void shutDown() {
        super.shutDown();
        listener.stop();
        context.status_message(String.format("Leaving workspace: %s.", url.toString()));
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
    }

    public void clear_highlights() {
        for (Entry<Integer, HashMap<Integer, LinkedList<RangeHighlighter>>> entry : highlights.entrySet()) {
            Integer user_id = entry.getKey();
            HashMap<Integer, LinkedList<RangeHighlighter>> value = entry.getValue();
            for (Entry<Integer, LinkedList<RangeHighlighter>> integerLinkedListEntry: value.entrySet()) {
                remove_highlight(user_id, integerLinkedListEntry.getKey(), null);
            }
        }
    }
}
