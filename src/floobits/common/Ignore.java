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
import java.util.*;
import java.util.Map.Entry;

public class Ignore {
    static String[] IGNORE_FILES = {".gitignore", ".hgignore", ".flignore", ".flooignore"};
    // TODO: make this configurable
    static final HashSet<String> HIDDEN_WHITE_LIST = new HashSet<String>(Arrays.asList(".gitignore", ".hgignore", ".flignore", ".flooignore", ".floo"));
    static final ArrayList<String> DEFAULT_IGNORES = new ArrayList<String>(Arrays.asList("extern", "node_modules", "tmp", "vendor", ".idea/workspace.xml", ".idea/misc"));
    static final int MAX_FILE_SIZE = 1024 * 1024 * 5;
    protected final VirtualFile file;
    private final int depth;
    protected final Ignore parent;
    protected final String stringPath;
    protected final HashMap<String, Ignore> children = new HashMap<String, Ignore>();
    protected final ArrayList<VirtualFile> files = new ArrayList<VirtualFile>();
    protected int size = 0;
    protected final HashMap<String, ArrayList<String>> ignores = new HashMap<String, ArrayList<String>>();

    public Ignore (VirtualFile virtualFile, Ignore parent, int depth) {
        this.file = virtualFile;
        this.depth = depth;
        this.stringPath = virtualFile.getPath();
        this.parent = parent;

        Flog.debug("Initializing ignores for %s", this.file);
        for (String name : IGNORE_FILES) {
            name = FilenameUtils.concat(this.file.getPath(), name);
            File ignoreFile = new File(name);
            String ignores[];
            try {
                ignores = FileUtils.readFileToString(ignoreFile, "UTF-8").split("\\r?\\n");
            } catch (IOException e) {
                continue;
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
            if (!file.isValid()) { // &&file.exists()) {
                continue;
            }
            if (optionalSparsePath != null && !(Utils.isChild(optionalSparsePath, stringPath) || Utils.isChild(stringPath, optionalSparsePath))) {
                continue;
            }
//                TODO: check parent ignores
            if (isIgnored(file)) {
                continue;
            }
            if (file.is(VFileProperty.SYMLINK) || file.is(VFileProperty.SPECIAL)) {
                continue;
            }
            if (file.isDirectory()) {
                Ignore child = new Ignore(file, this, depth+1);
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
        if (virtualFile.is(VFileProperty.HIDDEN) && !HIDDEN_WHITE_LIST.contains(virtualFile.getName())){
            Flog.log("Ignoring %s because it is hidden.", path);
            return true;
        }
        if (!virtualFile.isDirectory() && virtualFile.getLength() > MAX_FILE_SIZE) {
            Flog.log("Ignoring %s because it is too big (%s)", path, virtualFile.getLength());
            return true;
        }
        ArrayList<String> paths = new ArrayList<String>();
        while (!Utils.isSamePath(virtualFile.getPath(), Shared.colabDir)) {
            paths.add(0, virtualFile.getName());
            virtualFile = virtualFile.getParent();
        }
        paths.add(0, Shared.colabDir);
        return isIgnored(path, paths.toArray(new String[paths.size()]), 0);
    }

    private Boolean isIgnored(String path, String[] pathParts, int depth) {
        String currentPath = "";
        for (int i = 0; i < pathParts.length; i++) {
            String base_path = currentPath;
            currentPath = FilenameUtils.concat(currentPath, pathParts[i]);
            if (i <= this.depth) {
                continue;
            }
            String relPath = Utils.getRelativePath(currentPath, stringPath);
            for (Entry<String, ArrayList<String>> entry : ignores.entrySet()) {
                for (String pattern : entry.getValue()) {
                    String file_name =  pathParts[i];
                    if (pattern.startsWith("/")) {
                        if (Utils.isSamePath(base_path, stringPath) && FilenameUtils.wildcardMatch(file_name, pattern.substring(1))) {
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
                    if (!file_name.equals(relPath) && FilenameUtils.wildcardMatch(relPath, pattern)) {
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

    public static Ignore buildIgnoreTree() {
        return buildIgnoreTree(null);
    }

    public static Ignore buildIgnoreTree(@Nullable VirtualFile optionalSparsePath) {
        if (optionalSparsePath != null && !Utils.isShared(optionalSparsePath.getPath())) {
            return null;
        }
        VirtualFile fileByIoFile = LocalFileSystem.getInstance().findFileByIoFile(new File(Shared.colabDir));
        if (fileByIoFile == null || !fileByIoFile.isValid()) {
            return null;
        }

        Ignore root = new Ignore(fileByIoFile, null, 0);
        root.recurse(optionalSparsePath != null ? optionalSparsePath.getPath() : null);
        return root;
    }

    public static Boolean isPathIgnored(VirtualFile path) {
        Ignore ignore = buildIgnoreTree(path);
        return ignore != null && ignore.isIgnored(path);
    }
    public static  void writeDefaultIgnores(String path) {
        Flog.log("Creating default ignores.");
        path = FilenameUtils.concat(path, ".flooignore");

        try {
            File f = new File(path);
            if (f.exists()) {
                return;
            }
            FileUtils.writeLines(f, DEFAULT_IGNORES);
        } catch (IOException e) {
            Flog.warn(e);
        }

    }
}
