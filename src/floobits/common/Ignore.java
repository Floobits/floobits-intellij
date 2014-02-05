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
    static final String[] IGNORE_FILES = {".gitignore", ".hgignore", ".flignore", ".flooignore"};
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
            if (file.is(VFileProperty.SYMLINK) || file.is(VFileProperty.SPECIAL)) {
                continue;
            }
            if (optionalSparsePath != null && !(Utils.isChild(optionalSparsePath, stringPath) || Utils.isChild(stringPath, optionalSparsePath))) {
                continue;
            }
            if (isIgnoredDown(file)) {
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

    public boolean isIgnoredUp(VirtualFile virtualFile) {
        if (!virtualFile.isValid()){
            Flog.log("Ignoring %s because it is invalid.", virtualFile);
            return true;
        }
        String path = FilenameUtils.normalizeNoEndSeparator(virtualFile.getPath());
        if (isFlooIgnored(virtualFile, path)) {
            return true;
        }
        ArrayList<String> paths = new ArrayList<String>();
        do {
            path = virtualFile.getPath();
            paths.add(0, path);
            virtualFile = virtualFile.getParent();
        } while (!Utils.isSamePath(path, Shared.colabDir));
        return isIgnoredUp(path, paths);
    }

    public boolean isIgnoredDown(VirtualFile virtualFile) {
        if (!virtualFile.isValid()){
            Flog.log("Ignoring %s because it is invalid.", virtualFile);
            return true;
        }
        String full_path = FilenameUtils.normalizeNoEndSeparator(virtualFile.getPath());
        if (isFlooIgnored(virtualFile, full_path)) {
            return true;
        }
        ArrayList<String> paths = new ArrayList<String>();
        String path;
        do {
            path = virtualFile.getPath();
            paths.add(0, path);
            virtualFile = virtualFile.getParent();
        } while (!Utils.isSamePath(path, Shared.colabDir));
//        Todo we can make this more efficient by not rechecking the parent paths more than once
        return isIgnoredDown(full_path, paths);
    }

    private boolean isIgnoredUp(String path, ArrayList<String> pathParts) {
        if (isGitIgnored(path, pathParts)) {
            return true;
        }
        if (depth + 1 >= pathParts.size()) {
            return false;
        }
        String nextName = FilenameUtils.getName(pathParts.get(depth));
        Ignore ignore = this.children.get(nextName);
        return ignore != null && ignore.isIgnoredUp(path, pathParts);
    }

    private boolean isIgnoredDown(String path, ArrayList<String> pathParts) {
        return isGitIgnored(path, pathParts) || parent != null && parent.isIgnoredDown(path, pathParts);
    }
    private boolean isFlooIgnored(VirtualFile virtualFile, String path) {
        if (!Utils.isShared(path)) {
            Flog.log("Ignoring %s because it isn't shared.", path);
            return true;
        }
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
        return false;
    }

    protected Boolean isGitIgnored(String path, ArrayList<String> pathParts) {
        String currentPath = "";
        String parentPath;
        for (int i = depth + 1; i < pathParts.size(); i++) {
            parentPath = currentPath;
            currentPath = pathParts.get(i);
            String file_name =  FilenameUtils.getName(currentPath);
            String relPath = Utils.getRelativePath(currentPath, stringPath);
            for (Entry<String, ArrayList<String>> entry : ignores.entrySet()) {
                for (String pattern : entry.getValue()) {
                    if (pattern.startsWith("/")) {
                        if (Utils.isSamePath(parentPath, stringPath) && FilenameUtils.wildcardMatch(file_name, pattern.substring(1))) {
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
                    if (file_name.equals(relPath)) {
                        continue;
                    }
                    if (FilenameUtils.wildcardMatch(relPath, pattern)) {
                        Flog.log("Ignoring %s because %s in %s", path, pattern, entry.getKey());
                        return true;
                    }
                }
            }
        }
        return false;
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
        return ignore != null && ignore.isIgnoredUp(path);
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
