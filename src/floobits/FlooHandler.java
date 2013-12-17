package floobits;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.RefactoringFactory;
import com.intellij.ui.JBColor;
import dmp.diff_match_patch;
import dmp.diff_match_patch.Patch;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
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

//    public ??? temp_data;
//    public HashMap<Integer, Term> terms;
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

class CreateBufResponse extends GetBufResponse {
}

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

    private static diff_match_patch dmp = new diff_match_patch();

    public FlooPatch (String current, Buf buf) {
        // TODO: handle binary bufs
        LinkedList<Patch> patches = dmp.patch_make((String) buf.buf, current);

        this.path = buf.path;
        this.md5_before = buf.md5;
        this.md5_after = DigestUtils.md5Hex(current);
        this.id = buf.id;
        this.patch = dmp.patch_toText(patches);
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
        this.buf = (String) buf.buf;
        this.encoding = buf.encoding.toString();
    }
}

class FlooCreateBuf implements Serializable {
    public String name = "create_buf";
    public String buf;
    public String path;
    public String md5;
    public String encoding;
    
    public FlooCreateBuf (VirtualFile virtualFile) throws IOException {
        this.path = Utils.toProjectRelPath(virtualFile.getCanonicalPath());
        this.buf = new String(virtualFile.contentsToByteArray(), "UTF-8");
        this.md5 = DigestUtils.md5Hex(this.buf);
        this.encoding = "utf8";
    }
}

class FlooHighlight implements Serializable {
    public String name = "highlight";
    public Integer id;
    public Boolean ping = false;
    public Boolean summon = false;
    public List<List<Integer>> ranges;
    public Integer user_id;

    public FlooHighlight (Buf buf, List<List<Integer>> ranges, Boolean summon) {
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

abstract class DocumentFetcher {
    Boolean make_document = false;

    DocumentFetcher(Boolean make_document) {
        this.make_document = make_document;
    }

    abstract public void on_document(Document document);

    public void fetch(final String path) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
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
            }
        });
    }
}

class FlooHandler extends ConnectionInterface {
    protected static boolean is_joined  = false;
    protected static diff_match_patch dmp = new diff_match_patch();
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
        status_message(String.format(" You successfully joined %s ", url.toString()));
    }

    public void on_data (String name, JsonObject obj) throws Exception {
        String method_name = "_on_" + name;
        Method method;
        try {
            method = this.getClass().getDeclaredMethod(method_name, new Class[]{JsonObject.class});
        } catch (NoSuchMethodException e) {
            Flog.error(e);
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
        ProjectManager pm = ProjectManager.getInstance();

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
//            VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(path);
//            virtualFile.getChildren();
//            Module moduleForFile = ModuleUtil.findModuleForFile(virtualFile, project);
//            ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(moduleForFile).getModifiableModel();
//            modifiableModel.addContentEntry(virtualFile);
//            modifiableModel.commit();
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
//                Todo: pick a new name or something
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
                return ignore.isIgnored(virtualFile.getCanonicalPath()) || upload(virtualFile);
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

        Buf buf;
        byte [] bytes;
        HashMap<Buf, String> conflicts = new HashMap<Buf, String>();
        for (Map.Entry entry : ri.bufs.entrySet()) {

            Integer buf_id = (Integer) entry.getKey();
            RiBuf b = (RiBuf) entry.getValue();
            buf = Buf.createBuf(b.path, b.id, Encoding.from(b.encoding), b.md5);
            this.bufs.put(buf_id, buf);
            this.paths_to_ids.put(b.path, b.id);
            String path = Utils.absPath(b.path);
            VirtualFile virtualFile = localFileSystem.findFileByPath(path);
            if (virtualFile == null || !virtualFile.exists()) {
                this.send_get_buf(buf.id);
                continue;
            }

            try {
                bytes = virtualFile.contentsToByteArray();
            } catch (IOException e) {
                Flog.log("Can't read %s", virtualFile.getCanonicalPath());
                this.send_get_buf(buf.id);
                continue;
            }

            String contents;
            try {
                contents = new String(bytes, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Flog.log("Not handling binary stuff for now: %s", virtualFile.getCanonicalPath());
                continue;
            }

            String md5 = DigestUtils.md5Hex(contents);
            if (!md5.equals(buf.md5)) {
                conflicts.put(buf, contents);
                continue;
            }
            buf.buf = contents;
        }

        if (conflicts.size() == 0) {
            if (this.should_upload) {
                this.upload();
            }
            return;
        }
        String dialog;
        if (conflicts.size() > 15) {
            dialog = "<p>%d files are different.  Do you want to overwrite them (OK)?</p> ";
         } else {
            dialog = "<p>The following file(s) are different.  Do you want to overwrite them (OK)?</p><ul>";
            for (Map.Entry<Buf, String> entry : conflicts.entrySet()) {
                dialog += String.format("<li>%s</li>", entry.getKey().path);
            }
            dialog += "</ul>";
        }

        DialogBuilder.build("Resolve Conflicts", dialog, new RunLater(conflicts) {
            @Override
            @SuppressWarnings("unchecked")
            void run(Object... objects) {
                Boolean stomp = (Boolean) objects[0];
                HashMap<Buf, String> bufStringHashMap = (HashMap<Buf, String>) data;
                for (Map.Entry<Buf, String> entry : bufStringHashMap.entrySet()) {
                    Buf buf = entry.getKey();
                    if (stomp) {
                        FloobitsPlugin.flooHandler.send_get_buf(buf.id);
                    } else {
                        buf.buf = entry.getValue();
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
        FlooCreateBuf flooCreateBuf;
        try {
            flooCreateBuf = new FlooCreateBuf(virtualFile);
        } catch (IOException e) {
            Flog.error(e);
            return;
        }
        this.conn.write(flooCreateBuf);
    }

    protected void _on_get_buf (JsonObject obj) {
        // TODO: be nice about this and update the existing view
        Gson gson = new Gson();
        GetBufResponse res = gson.fromJson(obj, (Type) GetBufResponse.class);

        Buf b = this.bufs.get(res.id);
        b.set(res.buf, res.md5);
        b.update();
    }

    protected void _on_create_buf (JsonObject obj) {
//        TODO: be nice about this and update the existing view
        Gson gson = new Gson();
        GetBufResponse res = gson.fromJson(obj, (Type) CreateBufResponse.class);
        Buf buf = Buf.createBuf(res.path, res.id, res.buf, res.md5);
        this.bufs.put(buf.id, buf);
        this.paths_to_ids.put(buf.path, buf.id);
        buf.update();
    }

    protected void _on_patch (JsonObject obj) {
        FlooPatch res = new Gson().fromJson(obj, (Type) FlooPatch.class);
        Buf b = this.bufs.get(res.id);
        if (b.buf == null) {
            Flog.warn("no buffer");
            this.send_get_buf(res.id);
            return;
        }

        if (res.patch.length() == 0) {
            Flog.warn("wtf? no patches to apply. server is being stupid");
            return;
        }

        Flog.info("Got _on_patch");

        if (!b.md5.equals(res.md5_before)) {
            Flog.info("MD5 before mismatch (ours %s remote %s). Sending get_buf.", b.md5, res.md5_before);
            this.send_get_buf(res.id);
            return;
        }

        LinkedList patches;
        patches = (LinkedList) dmp.patch_fromText(res.patch);
        Object[] results = dmp.patch_apply(patches, (String) b.buf);
        final String text = (String) results[0];
        boolean[] boolArray = (boolean[]) results[1];

        boolean cleanPatch = true;
        for (boolean clean : boolArray) {
            if (!clean) {
                cleanPatch = false;
                break;
            }
        }

        if (!cleanPatch) {
            Flog.info("Patch not clean. Sending get_buf.");
            this.send_get_buf(res.id);
            return;
        }

        String md5After = DigestUtils.md5Hex(text);
        if (!md5After.equals(res.md5_after)) {
            Flog.info("MD5 after mismatch (ours %s remote %s). Sending get_buf.", md5After, res.md5_after);
            this.send_get_buf(res.id);
            return;
        }
        Flog.log("Patched %s", res.path);

        b.set(text, res.md5_after);
        b.update();
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
        final List<List<Integer>> ranges = res.ranges;
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

    protected void _on_rename_buf (JsonObject jsonObject) {
        String name = jsonObject.get("old_path").getAsString();
        String oldPath = Utils.absPath(name);
        String newPath = Utils.absPath(jsonObject.get("path").getAsString());
        PsiFile[] filesByName = FilenameIndex.getFilesByName(project, oldPath, GlobalSearchScope.projectScope(project));

        if (filesByName != null) {
            RefactoringFactory refactoringFactory = RefactoringFactory.getInstance(project);
            for (PsiFile psiFile : filesByName) {
                refactoringFactory.createRename(psiFile, newPath);
            }
            return;
        }
        File oldFile = new File(oldPath);
        File newFile = new File(newPath);
        VirtualFile fileByPath = LocalFileSystem.getInstance().findFileByPath(oldPath);
        if (fileByPath != null) {
            try {
                fileByPath.rename(FlooHandler.class, newPath);
                return;
            } catch (IOException e) {
                Flog.warn(e);
            }
        }
//            Just move the thing on disk
        try {
            FileUtils.moveFile(oldFile, newFile);
        } catch (IOException e) {
            Flog.error(e);
        }
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

    public void _on_msg (JsonObject jsonObject){
        String msg = jsonObject.get("data").getAsString();
        String username = jsonObject.get("username").getAsString();
        status_message(String.format("%s: %s", username, msg));
    }

    public void send_get_buf (Integer buf_id) {
        this.conn.write(new GetBufRequest(buf_id));
    }

    public void send_patch (String current, Buf buf) {
        FlooPatch req = new FlooPatch(current, buf);

        this.conn.write(req);
    }

    public void send_set_buf (Buf b) {
        this.conn.write(new FlooSetBuf(b));
    }

    public void send_summon (String current, Integer offset) {
        Buf buf = this.get_buf_by_path(current);
        List<List<Integer>> ranges = new ArrayList<List<Integer>>();
        ranges.add(Arrays.asList(offset, offset));
        this.conn.write(new FlooHighlight(buf, ranges, true));
    }

    public void untellij_moved(String path, String newPath) {
        Flog.log("%s - %s", path, newPath);
    }

    public void untellij_changed(String path, String current) {
        Buf buf = this.get_buf_by_path(path);

        if (buf == null || buf.buf == null) {
            Flog.info("buf isn't populated yet %s", path);
            return;
        }
        this.send_patch(current, buf);
        buf.set(current, DigestUtils.md5Hex(current));
    }

    public void untellij_selection_change(String path, TextRange[] textRanges) {
        Buf buf = this.get_buf_by_path(path);

        if (buf == null || buf.buf == null) {
            Flog.info("buf isn't populated yet %s", path);
            return;
        }
        List<List<Integer>> ranges = new ArrayList<List<Integer>>();

        for (TextRange r : textRanges) {
            ranges.add(Arrays.asList(r.getStartOffset(), r.getEndOffset()));
        }
        this.conn.write(new FlooHighlight(buf, ranges, false));
    }

    public void untellij_saved(String path) {
        Buf buf = this.get_buf_by_path(path);

        if (buf == null || buf.buf == null) {
            Flog.info("buf isn't populated yet %s", path);
            return;
        }

        this.conn.write(new FlooSaveBuf(buf.id));
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
        status_message(String.format(" Leaving workspace: %s.", url.toString()));
        this.conn.shut_down();
        is_joined = false;
    }
}
