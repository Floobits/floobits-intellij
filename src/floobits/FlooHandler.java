package floobits;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.JBColor;
import org.apache.commons.httpclient.HttpMethod;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.util.*;
import java.util.Map.Entry;

class FlooAuth implements Serializable {
    public String username;
    public String api_key;
    public String secret;

    public String room;
    public String room_owner;
    public String client = "IntelliJ";
    public String platform = System.getProperty("os.name");
    public String version = "0.10";
    public String[] supported_encodings = { "utf8", "base64" };

    public FlooAuth (Settings settings, String owner, String workspace) {
        this.username = settings.get("username");
        this.api_key = settings.get("api_key");
        this.room = workspace;
        this.room_owner = owner;
        this.secret = settings.get("secret");
    }
}

class RiBuf implements Serializable {
    public Integer id;
    public String md5;
    public String path;
    public String encoding;
}

class User implements Serializable {
    public String[] perms;
    public String client;
    public String platform;
    public Integer user_id;
    public String username;
    public String version;
}

class Tree implements Serializable {
    public HashMap<String, Integer> bufs;
    public HashMap<String, Tree> folders;
    public Tree (JsonObject obj) {
        this.bufs = new HashMap<String, Integer>();
        this.folders = new HashMap<String, Tree>();
        for (Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (value.isJsonPrimitive()) {
                this.bufs.put(key, Integer.parseInt(value.getAsString()));
            } else {
                this.folders.put(key, new Tree(value.getAsJsonObject()));
            }
        }
    }
}

class RoomInfoResponse implements Serializable {
    public String[] anon_perms;
    public Integer max_size;
    public String name;
    public String owner;
    public String[] perms;
    public String room_name;
    public Boolean secret;
    public HashMap<Integer, User> users;
    public HashMap<Integer, RiBuf> bufs;

}

class GetBufRequest implements Serializable {
    public String name = "get_buf";
    public Integer id;

    public GetBufRequest (Integer buf_id) {
        this.id = buf_id;
    }
}

class GetBufResponse implements Serializable {
    public String name;
    public Integer id;
    public String path;
    public String buf;
    public String encoding;
    public String md5;
}

class CreateBufResponse extends GetBufResponse {}

class FlooPatch implements Serializable {
    public String name = "patch";
    public Integer id;
    public Integer user_id;
    public String md5_after;
    public String md5_before;
    public String patch;

    // Deprecated
    public String path;
    public String username;


    public FlooPatch(){}

    public FlooPatch (String patch, String md5_before, Buf buf) {
        this.path = buf.path;
        this.md5_before = md5_before;
        this.md5_after = buf.md5;
        this.id = buf.id;
        this.patch = patch;
    }
}

class FlooSetBuf implements Serializable {
    public String name = "set_buf";
    public Integer id;
    public String buf;
    public String md5;
    public String encoding;

    public FlooSetBuf (Buf buf) {
        this.md5 = buf.md5;
        this.id = buf.id;
        this.buf = buf.serialize();
        this.encoding = buf.encoding.toString();
    }
}

class FlooCreateBuf implements Serializable {
    public String name = "create_buf";
    public String buf;
    public String path;
    public String md5;
    public String encoding;
    
    public FlooCreateBuf (Buf buf) {
        this.path = buf.path;
        this.buf = buf.serialize();
        this.md5 = buf.md5;
        this.encoding = buf.encoding.toString();
    }
}

class FlooHighlight implements Serializable {
    public String name = "highlight";
    public Integer id;
    public Boolean ping = false;
    public Boolean summon = false;
    public ArrayList<ArrayList<Integer>> ranges;
    public Integer user_id;

    public FlooHighlight(){}

    public FlooHighlight (Buf buf, ArrayList<ArrayList<Integer>> ranges, Boolean summon) {
        this.id = buf.id;
        if (summon != null) {
            this.summon = summon;
            this.ping = summon;
        }
        this.ranges = ranges;
    }
}

class FlooSaveBuf implements Serializable {
    public Integer id;
    public String name = "saved";

    FlooSaveBuf(Integer id) {
        this.id = id;
    }
}

class FlooDeleteBuf implements Serializable {
    public Integer id;
    public String name = "delete_buf";

    FlooDeleteBuf(Integer id) {
        this.id = id;
    }
}

class FlooRenameBuf implements Serializable {
    public Integer id;
    public String name = "rename_buf";
    public String path;

    FlooRenameBuf(Integer id, String path) {
        this.id = id;
        this.path = path;
    }
}

abstract class DocumentFetcher {
    Boolean make_document = false;

    DocumentFetcher(Boolean make_document) {
        this.make_document = make_document;
    }

    abstract public void on_document(Document document);

    public void fetch(final String path) {
        ThreadSafe.write(new Runnable() {
            public void run() {
                String absPath = Utils.absPath(path);

                VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(absPath);
                if (virtualFile == null || !virtualFile.exists()) {
                    Flog.info("no virtual file for %s", path);
                    return;
                }
                Document d = FileDocumentManager.getInstance().getCachedDocument(virtualFile);
                if (d == null && make_document) {
                    d = FileDocumentManager.getInstance().getDocument(virtualFile);
                }

                if (d == null) {
                    Flog.info("could not make document for %s", path);
                    return;
                }
                on_document(d);
            }
        });
    };
}

class FlooHandler extends ConnectionInterface {
    protected static boolean is_joined  = false;
    protected Boolean should_upload = false;
    protected Project project;
    protected Boolean stomp = false;
    protected HashMap<Integer, HashMap<Integer, LinkedList<RangeHighlighter>>> highlights =
            new HashMap<Integer, HashMap<Integer, LinkedList<RangeHighlighter>>>();
    protected Boolean stalking = false;
    protected String[] perms;
    protected Map<Integer, User> users = new HashMap<Integer, User>();
    protected HashMap<Integer, Buf> bufs = new HashMap<Integer, Buf>();
    protected HashMap<String, Integer> paths_to_ids = new HashMap<String, Integer>();
    protected Tree tree;
    protected FlooConn conn;
    protected Timeouts timeouts = Timeouts.create();

    protected void flash_message(final String message) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    public void run() {
                        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
                        if (statusBar == null) {
                            return;
                        }
                        JLabel jLabel = new JLabel(message);
                        statusBar.fireNotificationPopup(jLabel, JBColor.WHITE);
                    }
                });
            }
        });
    }

    protected void status_message(final String message, final NotificationType notificationType) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    public void run() {
                        Notifications.Bus.notify(new Notification("Floobits", "Floobits", message, notificationType), project);
                    }
                });
            }
        });
    }

    protected void status_message(String message) {
        Flog.log(message);
        status_message(message, NotificationType.INFORMATION);
    }

    protected void error_message(String message) {
        Flog.log(message);
        status_message(message, NotificationType.ERROR);
    }

    protected String get_username(Integer user_id) {
        User user = users.get(user_id);
        if (user == null) {
            return null;
        }
        return user.username;
    }

    public void on_connect () {
        this.conn.write(new FlooAuth(new Settings(), this.url.owner, this.url.workspace));
        status_message(String.format("You successfully joined %s ", url.toString()));
    }

    public void on_data (String name, JsonObject obj) throws Exception {
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
        Flog.log("Calling %s", method_name);
        method.invoke(this, objects);
    }

    public static FlooHandler getInstance() {
        return FloobitsPlugin.flooHandler;
    }

    public FlooHandler (Project p) {
        this.project = p;
        this.stomp = true;
        // TODO: Should upload should be an explicit argument to the constructor.
        this.should_upload = true;
        final String project_path = p.getBasePath();
        FlooUrl flooUrl = DotFloo.read(project_path);
        if (workspaceExists(flooUrl)) {
            joinWorkspace(flooUrl, project_path);
            return;
        }

        PersistentJson persistentJson = PersistentJson.getInstance();
        for (Entry<String, Map<String, Workspace>> i : persistentJson.workspaces.entrySet()) {
            Map<String, Workspace> workspaces = i.getValue();
            for (Entry<String, Workspace> j : workspaces.entrySet()) {
                Workspace w = j.getValue();
                if (Utils.isSamePath(w.path, project_path)) {
                    try {
                        flooUrl = new FlooUrl(w.url);
                    } catch (MalformedURLException e) {
                        Flog.error(e);
                        continue;
                    }
                    if (workspaceExists(flooUrl)) {
                        joinWorkspace(flooUrl, project_path);
                        return;
                    }
                }
            }
        }

        Settings settings = new Settings();
        String owner = settings.get("username");
        final String name = new File(project_path).getName();
        List<String> orgs = API.getOrgsCanAdmin();

        if (orgs.size() == 0) {
            createWorkspace(owner, name, project_path);
            return;
        }

        orgs.add(0, owner);
        SelectOwner.build(orgs, new RunLater(null) {
            @Override
            void run(Object... objects) {
                String owner = (String) objects[0];
                createWorkspace(owner, name, project_path);
            }
        });
    }
    public FlooHandler (final FlooUrl flooUrl) {
        if (!workspaceExists(flooUrl)) {
            error_message(String.format("The workspace %s does not exist!", flooUrl.toString()));
        }

        PersistentJson p = PersistentJson.getInstance();
        String path;
        try {
            path = p.workspaces.get(flooUrl.owner).get(flooUrl.workspace).path;
        } catch (Exception e) {
            SelectFolder.build(Utils.unFuckPath("~"), new RunLater(null) {
                @Override
                void run(Object... objects) {
                    File file = (File)objects[0];
                    String path;
                    try {
                        path = file.getCanonicalPath();
                    } catch (IOException e) {
                        Flog.error(e);
                        return;
                    }
                    joinWorkspace(flooUrl, path);
                }
            });
            return;
        }
        ProjectManager pm = ProjectManager.getInstance();
        // Check open projects
        Project[] openProjects = pm.getOpenProjects();
        for (Project project : openProjects) {
            if (path.equals(project.getBasePath())) {
                this.project = project;
                break;
            }
        }

        // Try to open existing project
        if (this.project == null) {
            try {
                this.project = pm.loadAndOpenProject(path);
            } catch (Exception e) {
                Flog.error(e);
            }
        }

        // Create project
        if (this.project == null) {
            this.project = pm.createProject(this.url.workspace, path);
            try {
                ProjectManager.getInstance().loadAndOpenProject(this.project.getBasePath());
            } catch (Exception e) {
                Flog.error(e);
                return;
            }
        }
        joinWorkspace(flooUrl, this.project.getBasePath());
    }

    public Boolean workspaceExists(final FlooUrl f) {
        if (f == null) {
            return false;
        }
        HttpMethod method;
        try {
            method = API.getWorkspace(f.owner, f.workspace);
        } catch (IOException e) {
            Flog.warn(e);
            return false;
        }

        return method.getStatusCode() < 400;
    }

    public void joinWorkspace(final FlooUrl f, final String path) {
        Flog.warn("join workspace");
        url = f;
        Shared.colabDir = path;
        PersistentJson persistentJson = PersistentJson.getInstance();
        persistentJson.addWorkspace(f, path);
        persistentJson.save();
        conn = new FlooConn(this);
        conn.start();
    }

    protected void createWorkspace(String owner, String name, String project_path) {
        HttpMethod method;
        try {
            method = API.createWorkspace(owner, name);
        } catch (IOException e) {
            error_message(String.format("Could not create workspace: %s", e.toString()));
            return;
        }
        int code = method.getStatusCode();
        switch (code) {
            case 400:
                // Todo: pick a new name or something
                error_message("Invalid workspace name.");
                return;
            case 402:
                String details;
                try {
                    String res = method.getResponseBodyAsString();
                    JsonObject obj = (JsonObject)new JsonParser().parse(res);
                    details = obj.get("detail").getAsString();
                } catch (IOException e) {
                    Flog.error(e);
                    return;
                }
                error_message(details);
                return;
            case 409:
                Flog.warn("The workspace already exists so I am joining it.");
            case 201:
                Flog.log("Workspace created.");
                joinWorkspace(new FlooUrl(Shared.defaultHost, owner, name, -1, true), project_path);
                return;
            default:
                try {
                    Flog.error(String.format("Unknown error creating workspace:\n%s", method.getResponseBodyAsString()));
                } catch (IOException e) {
                    Flog.error(e);
                }
        }
    }

    protected Buf get_buf_by_path(String absPath) {
        String relPath = Utils.toProjectRelPath(absPath);
        if (relPath == null) {
            return null;
        }
        Integer id = this.paths_to_ids.get(relPath);
        if (id == null) {
            return null;
        }
        return this.bufs.get(id);
    }

    public void upload() {
        final Ignore ignore;
        try {
            ignore = new Ignore(new File(Shared.colabDir), null, false);
        } catch (Exception ex) {
            Flog.error(ex);
            return;
        }

        ProjectRootManager.getInstance(project).getFileIndex().iterateContent(new ContentIterator() {
            public boolean processFile(final VirtualFile virtualFile) {
                if (!ignore.isIgnored(virtualFile.getCanonicalPath())) upload(virtualFile);
                return true;
            }
        });
    }

    public boolean upload(VirtualFile virtualFile) {
        if (!Utils.isSharableFile(virtualFile)) {
            return true;
        }
        String path = virtualFile.getPath();
        if (!Utils.isShared(path)) {
            Flog.info("Thing isn't shared: %s", path);
            return true;
        }
        String rel_path = Utils.toProjectRelPath(path);
        if (rel_path.equals(".idea/workspace.xml")) {
            Flog.info("Not sharing the workspace.xml file");
            return true;
        }

        Buf b = FloobitsPlugin.flooHandler.get_buf_by_path(path);
        if (b != null) {
            Flog.info("Already in workspace: %s", path);
            return true;
        }
        FloobitsPlugin.flooHandler.send_create_buf(virtualFile);
        return true;
    }



    protected void _on_room_info (JsonObject obj) {
        RoomInfoResponse ri = new Gson().fromJson(obj, (Type) RoomInfoResponse.class);
        is_joined = true;
        this.tree = new Tree(obj.getAsJsonObject("tree"));
        this.users = ri.users;
        this.perms = ri.perms;
        LocalFileSystem localFileSystem = LocalFileSystem.getInstance();

        DotFloo.write(this.url.toString());

        LinkedList<Buf> conflicts = new LinkedList<Buf>();
        for (Map.Entry entry : ri.bufs.entrySet()) {
            Integer buf_id = (Integer) entry.getKey();
            RiBuf b = (RiBuf) entry.getValue();
            Buf buf = Buf.createBuf(b.path, b.id, Encoding.from(b.encoding), b.md5);
            this.bufs.put(buf_id, buf);
            this.paths_to_ids.put(b.path, b.id);
            buf.read();
            if (buf.buf == null) {
                this.send_get_buf(buf.id);
                continue;
            }
            if (!b.md5.equals(buf.md5)) {
                conflicts.add(buf);
                continue;
            }
        }

        if (conflicts.size() == 0) {
            if (this.should_upload) {
                this.upload();
            }
            return;
        }
        String dialog;
        if (conflicts.size() > 15) {
            dialog = String.format("<p>%d files are different.  Do you want to overwrite them (OK)?</p> ", conflicts.size());
         } else {
            dialog = "<p>The following file(s) are different.  Do you want to overwrite them (OK)?</p><ul>";
            for (Buf buf : conflicts) {
                dialog += String.format("<li>%s</li>", buf.path);
            }
            dialog += "</ul>";
        }

        DialogBuilder.build("Resolve Conflicts", dialog, new RunLater(conflicts) {
            @Override
            @SuppressWarnings("unchecked")
            void run(Object... objects) {
                Boolean stomp = (Boolean) objects[0];
                LinkedList<Buf> conflicts = (LinkedList<Buf>) data;
                for (Buf buf : conflicts) {
                    if (stomp) {
                        FloobitsPlugin.flooHandler.send_get_buf(buf.id);
                        buf.buf = null;
                        buf.md5 = null;
                    } else {
                        FloobitsPlugin.flooHandler.send_set_buf(buf);
                    }
                }
                if (should_upload) {
                    FloobitsPlugin.flooHandler.upload();
                }
            }
        });

    }

    public void send_create_buf(VirtualFile virtualFile) {
        Buf buf = Buf.createBuf(virtualFile);
        if (buf == null) {
            return;
        }
        this.conn.write(new FlooCreateBuf(buf));
    }

    protected void _on_get_buf (JsonObject obj) {
        // TODO: be nice about this and update the existing view
        Gson gson = new Gson();
        GetBufResponse res = gson.fromJson(obj, (Type) GetBufResponse.class);

        Buf b = this.bufs.get(res.id);
        b.set(res.buf, res.md5);
        b.write();
        Flog.info("on get buffed. %s", b.path);
    }

    protected void _on_create_buf (JsonObject obj) {
        // TODO: be nice about this and update the existing view
        Gson gson = new Gson();
        GetBufResponse res = gson.fromJson(obj, (Type) CreateBufResponse.class);
        Buf buf = Buf.createBuf(res.path, res.id, res.buf, res.md5);
        this.bufs.put(buf.id, buf);
        this.paths_to_ids.put(buf.path, buf.id);
        buf.write();
    }

    protected void _on_patch (JsonObject obj) {
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

    public void get_document(Integer id, DocumentFetcher documentFetcher) {
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

    public void get_document(String path, DocumentFetcher documentFetcher) {
        documentFetcher.fetch(path);
    }

    protected Editor get_editor_for_document(Document document) {
        Editor[] editors = EditorFactory.getInstance().getEditors(document, project);
        if (editors.length > 0) {
            return editors[0];
        }
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        if (virtualFile == null) {
            return null;
        }
        return EditorFactory.getInstance().createEditor(document, project, virtualFile, true);
    }

    public void remove_highlight (Integer userId, Integer bufId, Document document) {
        HashMap<Integer, LinkedList<RangeHighlighter>> integerRangeHighlighterHashMap = FloobitsPlugin.flooHandler.highlights.get(userId);
        if (integerRangeHighlighterHashMap == null) {
            return;
        }
        final LinkedList<RangeHighlighter> rangeHighlighters = integerRangeHighlighterHashMap.get(bufId);
        if (rangeHighlighters == null) {
            return;
        }
        if (document != null) {
            Editor editor = get_editor_for_document(document);
            MarkupModel markupModel = editor.getMarkupModel();

            for (RangeHighlighter rangeHighlighter: rangeHighlighters) {
                markupModel.removeHighlighter(rangeHighlighter);
            }
            rangeHighlighters.clear();
            return;
        }

        FloobitsPlugin.flooHandler.get_document(bufId, new DocumentFetcher(false) {
            @Override
            public void on_document(Document document) {
                Editor editor = get_editor_for_document(document);
                MarkupModel markupModel = editor.getMarkupModel();

                for (RangeHighlighter rangeHighlighter: rangeHighlighters) {
                    markupModel.removeHighlighter(rangeHighlighter);
                }
                rangeHighlighters.clear();
            }
        });

    }

    protected void _on_highlight (JsonObject obj) {
        final FlooHighlight res = new Gson().fromJson(obj, (Type)FlooHighlight.class);
        final ArrayList<ArrayList<Integer>> ranges = res.ranges;
        final Boolean force = FloobitsPlugin.flooHandler.stalking || res.ping || (res.summon == null ? Boolean.FALSE : res.summon);
        FloobitsPlugin.flooHandler.get_document(res.id, new DocumentFetcher(force) {
            @Override
            public void on_document(Document document) {
                final FileEditorManager manager = FileEditorManager.getInstance(project);
                VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
                if (force && virtualFile != null) {
                    String username = get_username(res.user_id);
                    if (username != null) {
                        status_message(String.format("%s has summoned you to %s", username, virtualFile.getPath()));
                    }
                    manager.openFile(virtualFile, true, true);
                }
                FloobitsPlugin.flooHandler.remove_highlight(res.user_id, res.id, document);

                int textLength = document.getTextLength();
                if (textLength == 0) {
                    return;
                }
                TextAttributes attributes = new TextAttributes();
                attributes.setEffectColor(JBColor.GREEN);
                attributes.setEffectType(EffectType.SEARCH_MATCH);
                attributes.setBackgroundColor(JBColor.GREEN);

                boolean first = true;
                Editor editor = FloobitsPlugin.flooHandler.get_editor_for_document(document);
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

                    RangeHighlighter rangeHighlighter = markupModel.addRangeHighlighter(start, end, HighlighterLayer.ERROR + 100,
                            attributes, HighlighterTargetArea.EXACT_RANGE);

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
                HashMap<Integer, LinkedList<RangeHighlighter>> integerRangeHighlighterHashMap = FloobitsPlugin.flooHandler.highlights.get(res.user_id);

                if (integerRangeHighlighterHashMap == null) {
                    integerRangeHighlighterHashMap = new HashMap<Integer, LinkedList<RangeHighlighter>>();
                    FloobitsPlugin.flooHandler.highlights.put(res.user_id, integerRangeHighlighterHashMap);
                }
                integerRangeHighlighterHashMap.put(res.id, rangeHighlighters);
            }
        });
    }

    protected void _on_saved (JsonObject obj) {
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

    protected void _on_rename_buf (JsonObject jsonObject) {
        final String name = jsonObject.get("old_path").getAsString();
        final String oldPath = Utils.absPath(name);
        final String newPath = Utils.absPath(jsonObject.get("path").getAsString());
        final VirtualFile foundFile = LocalFileSystem.getInstance().findFileByPath(oldPath);
        Buf buf = get_buf_by_path(oldPath);
        if (buf == null) {
            if (get_buf_by_path(newPath) == null) {
                Flog.error("Rename oldPath and newPath don't exist. %s %s", oldPath, newPath);
            } else {
                Flog.info("We probably renamed this, nothing to rename.");
            }
            return;
        }
        if (foundFile == null) {
            Flog.warn("File we want to move was not found %s %s.", oldPath, newPath);
            return;
        }
        String newRelativePath = Utils.toProjectRelPath(newPath);
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
                            Flog.error("Error renaming file %s %s %s", e, oldPath, newPath);
                        }
                        // Move file
                        String newParentDirectoryPath = newFile.getParent();
                        String oldParentDirectoryPath = oldFile.getParent();
                        if (newParentDirectoryPath == oldParentDirectoryPath) {
                            Flog.info("Only renamed file, don't need to move %s %s", oldPath, newPath);
                            return;
                        }
                        VirtualFile directory = null;
                        try {
                            directory = VfsUtil.createDirectories(newParentDirectoryPath);
                        } catch (IOException e) {
                            Flog.error("Failed to create directories in time for moving file. %s %s", oldPath, newPath);

                        }
                        if (directory == null) {
                            Flog.error("Failed to create directories in time for moving file. %s %s", oldPath, newPath);
                            return;
                        }
                        try {
                            foundFile.move(null, directory);
                        } catch (IOException e) {
                            Flog.error("Error moving file %s %s %s", e,oldPath, newPath);
                        }
                    }
                });
            }
        });
    }

    protected void _on_request_perms(JsonObject obj) {
        Flog.log("got perms request");
    }

    protected void _on_join (JsonObject obj) {
        User u = new Gson().fromJson(obj, (Type)User.class);
        this.users.put(u.user_id, u);
        status_message(String.format("%s joined the workspace on %s (%s).", u.username, u.platform, u.client));
    }

    protected void _on_part (JsonObject obj) {
        Integer userId = obj.get("user_id").getAsInt();
        User u = users.get(userId);
        this.users.remove(userId);
        HashMap<Integer, LinkedList<RangeHighlighter>> integerRangeHighlighterHashMap = FloobitsPlugin.flooHandler.highlights.get(userId);
        if (integerRangeHighlighterHashMap == null) {
            return;
        }
        for (Entry<Integer, LinkedList<RangeHighlighter>> entry : integerRangeHighlighterHashMap.entrySet()) {
            remove_highlight(userId, entry.getKey(), null);
        }
        status_message(String.format("%s left the workspace.", u.username));

    }

    protected void _on_disconnect (JsonObject jsonObject) {
        is_joined = false;
        String reason = jsonObject.get("reason").getAsString();
        if (reason != null) {
            error_message(reason);
            flash_message(reason);
        } else {
            status_message("You have left the workspace");
        }
        FloobitsPlugin.flooHandler = null;
        conn.shut_down();
    }

    protected void _on_delete_buf(JsonObject jsonObject) {
        Integer id = jsonObject.get("id").getAsInt();
        Buf buf = bufs.get(id);
        if (buf == null) {
            Flog.warn(String.format("Tried to delete a buf that doesn't exist: %s", id));
            return;
        }
        String absPath = Utils.absPath(buf.path);
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
                            fileByPath.delete(FlooHandler.getInstance());
                        } catch (IOException e) {
                            Flog.error(e);
                        }
                    }
                });
            }
        });
    }

    public void _on_msg (JsonObject jsonObject){
        String msg = jsonObject.get("data").getAsString();
        String username = jsonObject.get("username").getAsString();
        status_message(String.format("%s: %s", username, msg));
    }

    public void _on_term_stdout(JsonObject jsonObject) {}
    public void _on_term_stdin(JsonObject jsonObject) {}

    public void send_get_buf (Integer buf_id) {
        this.conn.write(new GetBufRequest(buf_id));
    }


    public void send_patch (String textPatch, String before_md5, TextBuf buf) {
        FlooPatch req = new FlooPatch(textPatch, before_md5, buf);
        this.conn.write(req);
    }

    public void send_set_buf (Buf b) {
        this.conn.write(new FlooSetBuf(b));
    }

    public void send_summon (String current, Integer offset) {
        Buf buf = this.get_buf_by_path(current);
        ArrayList<ArrayList<Integer>> ranges = new ArrayList<ArrayList<Integer>>();
        ranges.add(new ArrayList<Integer>(Arrays.asList(offset, offset)));
        this.conn.write(new FlooHighlight(buf, ranges, true));
    }

    public void untellij_renamed(String path, String newPath) {
        Flog.log("Renamed buf: %s - %s", path, newPath);
        Buf buf = this.get_buf_by_path(path);
        if (buf == null) {
            Flog.info("buf does not exist.");
            return;
        }
        String newRelativePath = Utils.toProjectRelPath(newPath);
        if (buf.path.equals(newRelativePath)) {
            Flog.info("untellij_renamed handling workspace rename, aborting.");
            return;
        }
        buf.cancelTimeout();
        this.conn.write(new FlooRenameBuf(buf.id, newRelativePath));
        set_buf_path(buf, newRelativePath);
    }

    public void untellij_changed(VirtualFile file) {
        Buf buf = this.get_buf_by_path(file.getPath());

        if (buf == null || !buf.isPopulated()) {
            Flog.info("buf isn't populated yet %s", buf);
            return;
        }
        buf.send_patch(file);
    }

    public void untellij_selection_change(String path, ArrayList<ArrayList<Integer>> textRanges) {
        Buf buf = this.get_buf_by_path(path);

        if (buf == null || buf.buf == null) {
            Flog.info("buf isn't populated yet %s", path);
            return;
        }
        this.conn.write(new FlooHighlight(buf, textRanges, false));
    }

    public void untellij_saved(String path) {
        Buf buf = this.get_buf_by_path(path);

        if (buf == null || buf.buf == null) {
            Flog.info("buf isn't populated yet %s", path);
            return;
        }

        this.conn.write(new FlooSaveBuf(buf.id));
    }

    public void untellij_deleted(String path) {
        Buf buf = this.get_buf_by_path(path);
        if (buf == null) {
            Flog.info("buf does not exist");
            return;
        }
        buf.cancelTimeout();
        this.conn.write(new FlooDeleteBuf(buf.id));
    }

    public void untellij_deleted_directory(ArrayList<String> filePaths) {
        for (String filePath : filePaths) {
            untellij_deleted(filePath);
        }
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
    }

    public void clear_highlights() {
        for (Entry<Integer, HashMap<Integer, LinkedList<RangeHighlighter>>> entry : FloobitsPlugin.flooHandler.highlights.entrySet()) {
            Integer user_id = entry.getKey();
            HashMap<Integer, LinkedList<RangeHighlighter>> value = entry.getValue();
            for (Entry<Integer, LinkedList<RangeHighlighter>> integerLinkedListEntry: value.entrySet()) {
                remove_highlight(user_id, integerLinkedListEntry.getKey(), null);
            }
        }
    }


    public void shut_down() {
        status_message(String.format("Leaving workspace: %s.", url.toString()));
        this.conn.shut_down();
        is_joined = false;
    }
}
