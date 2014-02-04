package floobits.common;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.utilities.Flog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

public class Ignore {
    static String[] IGNORE_FILES = {".gitignore", ".hgignore", ".flignore", ".flooignore"};
    // TODO: make this configurable
    static public final HashSet<String> HIDDEN_WHITELIST = new HashSet<String>(
            Arrays.asList(".gitignore", ".hgignore", ".flignore", ".flooignore", ".floo"));

    //TODO: grab global git ignores:
    static String[] DEFAULT_IGNORES = {"extern", "node_modules", "tmp", "vendor", ".idea/workspace.xml"};
    static int MAX_FILE_SIZE = 1024 * 1024 * 5;

    protected VirtualFile file;
    private int depth;
    protected String unfuckedPath;
    protected Ignore parent;
    protected String stringPath;
    protected HashMap<String, Ignore> children = new HashMap<String, Ignore>();
    protected ArrayList<VirtualFile> files = new ArrayList<VirtualFile>();
    protected int size;
    protected HashMap<String, ArrayList<String>> ignores = new HashMap<String, ArrayList<String>>();

    public Ignore (VirtualFile virtualFile, Ignore parent, int depth) {
        this.file = virtualFile;
        this.depth = depth;
        this.stringPath = virtualFile.getPath();
        this.unfuckedPath = file.getPath();
        this.parent = parent;
//        this.ignores.put("/TOO_BIG/", new ArrayList<String>());

        Flog.debug("Initializing ignores for %s", this.file);
        for (String name : IGNORE_FILES) {
            name = FilenameUtils.concat(this.file.getPath(), name);
            File ignoreFile = new File(name);
            this.loadIgnore(ignoreFile);
        }
    }

    public ArrayList<VirtualFile> getFiles() {
        ArrayList<VirtualFile> arrayList= new ArrayList<VirtualFile>();
        arrayList.addAll(files);
        for (Entry<String, Ignore> entry: children.entrySet()) {
            arrayList.addAll(entry.getValue().getFiles());
        }
        return arrayList;
    }

    @SuppressWarnings("UnsafeVfsRecursion")
    public void recurse(String optionalSparsePath) {
        @SuppressWarnings("UnsafeVfsRecursion") VirtualFile[] children = file.getChildren();
        if (children == null) {
            return;
        }
        for (VirtualFile file : children) {
            if (optionalSparsePath != null && !Utils.isChild(optionalSparsePath, file.getPath())) {
                continue;
            }
//                TODO: check parent ignores
            if (isIgnored(file)) {
                continue;
            }
            if (!file.isValid()) {
                continue;
            }
            if (file.is(VFileProperty.SYMLINK)) {
                continue;
            }
            if (file.isDirectory()) {
                Ignore child;
                try {
                    child = new Ignore(file, this, depth+1);
                } catch (Exception ignored) {
                    continue;
                }
                this.children.put(file.getName(), child);
                child.recurse(optionalSparsePath);
                size += child.size;
            }
            if (file.is(VFileProperty.SPECIAL)) {
                continue;
            }
            files.add(file);
            size += file.getLength();
        }
    }

    protected void loadIgnore (File file) {
        String ignores[];
        try {
            ignores = FileUtils.readFileToString(file, "UTF-8").split("\\r?\\n");
        } catch (IOException e) {
            return;
        }

        ArrayList<String> igs = new ArrayList<String>();
        for (String ignore: ignores) {
            ignore = ignore.trim();
            if (ignore.length() == 0 || ignore.startsWith("#")) {
                continue;
            }
            igs.add(ignore);
        }
        this.ignores.put(file.getName(), igs);
    }

    private Boolean isIgnored(String path, String[] pathParts, int depth) {
        String currentPath = "";
        for (int i = 0; i < pathParts.length; i++) {
            String base_path = currentPath;
            base_path = Utils.absPath(base_path);
            currentPath = FilenameUtils.concat(currentPath, pathParts[i]);
            if (i <= this.depth) {
                continue;
            }
            String relPath = Utils.getRelativePath(currentPath, this.file.getPath());
            for (Entry<String, ArrayList<String>> entry : ignores.entrySet()) {
                for (String pattern : entry.getValue()) {
                    String file_name =  pathParts[i];
                    if (pattern.startsWith("/")) {
                        if (Utils.isSamePath(base_path, unfuckedPath) && FilenameUtils.wildcardMatch(file_name, pattern.substring(1))) {
                            Flog.log("Ignoring %s because %s in %s", path, pattern, entry.getKey());
                            return true;
                        }
                        continue;
                    }
                    if (pattern.endsWith("/") && new File(path).isDirectory()) {
                        pattern = pattern.substring(0, pattern.length() - 1);
                    }
                    if (FilenameUtils.wildcardMatch(file_name, pattern)) {
                        Flog.log("Ignoring %s because %s in %s", path, pattern, entry.getKey());
                        return true;
                    }
                    if (FilenameUtils.wildcardMatch(relPath, pattern)) {
                        Flog.log("Ignoring %s because %s in %s", path, pattern, entry.getKey());
                        return true;
                    }
                }
            }
        }
        if (depth >= pathParts.length) {
            return false;
        }
        depth += 1;
        String nextName = pathParts[depth];
        Ignore ignore = this.children.get(nextName);
        if (ignore == null) {
            return false;
        }
        return ignore.isIgnored(path, pathParts, depth);
    }

    public Boolean isIgnored(VirtualFile virtualFile) {
        if (!virtualFile.isValid()){
            Flog.log("Ignoring %s because it is invalid.", virtualFile);
            return true;
        }
        String path = virtualFile.getPath();
        if (!Utils.isShared(path)) {
            Flog.log("Ignoring %s because it isn't shared.", path);
            return true;
        }
        path = FilenameUtils.normalizeNoEndSeparator(path);
        if (path.equals(stringPath)) {
            return false;
        }
        if (virtualFile.is(VFileProperty.SPECIAL) || virtualFile.is(VFileProperty.SYMLINK)) {
            Flog.log("Ignoring %s because it is special or a symlink.", path);
            return true;
        }
        if (virtualFile.is(VFileProperty.HIDDEN) && !HIDDEN_WHITELIST.contains(virtualFile.getName())){
            Flog.log("Ignoring %s because it is hidden.", path);
            return true;
        }
        if (!virtualFile.isDirectory() && virtualFile.getLength() > MAX_FILE_SIZE) {
            Flog.log("Ignoring %s because it is too big (%s)", path, virtualFile.getLength());
            return true;
        }
        ArrayList<String> paths = new ArrayList<String>();
        while (!Utils.isSamePath(path, Shared.colabDir)) {
            paths.add(0, virtualFile.getName());
            virtualFile = virtualFile.getParent();
        }
        paths.add(0, Shared.colabDir);
        return isIgnored(path, paths.toArray(new String[paths.size()]), 0);
    }

    public static Ignore buildIgnoreTree() {
        return buildIgnoreTree(null);
    }

    public static Ignore buildIgnoreTree(@Nullable VirtualFile optionalSparsePath) {
        if (optionalSparsePath != null && !Utils.isShared(optionalSparsePath.getPath())) {
            return null;
        }

        if (optionalSparsePath == null) {
            optionalSparsePath = LocalFileSystem.getInstance().findFileByIoFile(new File(Shared.colabDir));
            if (optionalSparsePath == null) {
                return  null;
            }
        }

        Ignore root = new Ignore(optionalSparsePath, null, 0);
        root.recurse(optionalSparsePath.getPath());
        return root;
    }

    public static Boolean isPathIgnored(VirtualFile path) {
        Ignore ignore = buildIgnoreTree(path);
        return ignore != null && ignore.isIgnored(path);
    }
}
