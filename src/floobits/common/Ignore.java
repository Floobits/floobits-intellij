package floobits.common;

import com.intellij.openapi.vfs.VirtualFile;
import floobits.utilities.Flog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class Ignore {
    static String[] IGNORE_FILES = {".gitignore", ".hgignore", ".flignore", ".flooignore"};
    // TODO: make this configurable
    static public final HashSet<String> HIDDEN_WHITELIST = new HashSet<String>(
            Arrays.asList(".gitignore", ".hgignore", ".flignore", ".flooignore", ".floo"));

    //TODO: grab global git ignores:
    static String[] DEFAULT_IGNORES = {"extern", "node_modules", "tmp", "vendor", ".idea/workspace.xml"};
    static int MAX_FILE_SIZE = 1024 * 1024 * 5;

    protected File file;
    private int depth;
    protected String unfuckedPath;
    protected Ignore parent;
    protected String stringPath;
    protected HashMap<String, Ignore> children = new HashMap<String, Ignore>();
    protected ArrayList<File> files = new ArrayList<File>();
    protected int size;
    protected HashMap<String, ArrayList<String>> ignores = new HashMap<String, ArrayList<String>>();

    public Ignore (File basePath, Ignore parent, int depth) throws IOException {
        this.file = basePath;
        this.depth = depth;
        this.stringPath = basePath.getPath();
        unfuckedPath = this.file.getPath();
        this.parent = parent;
//        this.ignores.put("/TOO_BIG/", new ArrayList<String>());

        Flog.debug("Initializing ignores for %s", this.file);
        for (String name : IGNORE_FILES) {
            name = FilenameUtils.concat(this.file.getPath(), name);
            File ignoreFile = new File(name);
            this.loadIgnore(ignoreFile);
        }
    }

    public ArrayList<File> getFiles() {
        ArrayList<File> arrayList= new ArrayList<File>();
        arrayList.addAll(files);
        for (Entry<String, Ignore> entry: children.entrySet()) {
            arrayList.addAll(entry.getValue().getFiles());
        }
        return arrayList;
    }

    public void recurse(String optionalSparsePath) {
        File[] children = file.listFiles();
        if (children == null) {
            return;
        }
        for (File file : children) {
            if (optionalSparsePath != null && !Utils.isChild(optionalSparsePath, file.getPath())) {
                continue;
            }
//                TODO: check parent ignores
            if (isIgnored(file.getPath())) {
                continue;
            }
            if (file.isFile()) {
                files.add(file);
                size += file.length();
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
            currentPath = FilenameUtils.concat(currentPath, pathParts[i]);
            if (i <= this.depth) {
                continue;
            }
            String relPath = Utils.getRelativePath(currentPath, this.file.getPath());
            File relPathFile = new File(currentPath);
            for (Entry<String, ArrayList<String>> entry : ignores.entrySet()) {
                for (String pattern : entry.getValue()) {
                    String base_path = relPathFile.getParent();
                    if (base_path == null) {
                        base_path = "";
                    }
                    base_path = Utils.absPath(base_path);
                    String file_name =  relPathFile.getName();
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

    public Boolean isIgnored(String path) {
        if (!Utils.isShared(path)) {
            return true;
        }
        path = FilenameUtils.normalizeNoEndSeparator(path);
        if (path.equals(this.stringPath)) {
            return false;
        }

        File f = new File(path);

        if (file.isFile() && file.length() > MAX_FILE_SIZE) {
            Flog.log("Ignoring %s because it is too big (%s)", file.getPath(), file.length());
            return true;
        }
        if (file.isHidden() && !HIDDEN_WHITELIST.contains(file.getName())){
            Flog.log("Ignoring %s because it is hidden.", file.getPath());
            return true;
        }
        ArrayList<String> paths = new ArrayList<String>();
        while (!Utils.isSamePath(f.getPath(), Shared.colabDir)) {
            paths.add(0, f.getName());
            f = f.getParentFile();
        }
        paths.add(0, Shared.colabDir);
        return isIgnored(path, paths.toArray(new String[paths.size()]), 0);
    }

    public static Ignore buildIgnoreTree() {
        return buildIgnoreTree(null);
    }

    public static Ignore buildIgnoreTree(final String optionalSparsePath) {
        if (optionalSparsePath != null && !Utils.isShared(optionalSparsePath)) {
            return null;
        }

        Ignore root;
        try {
            root = new Ignore(new File(Shared.colabDir), null, 0);
        } catch (IOException e) {
            return null;
        }

        root.recurse(optionalSparsePath);
        return root;
    }

    public static Boolean isPathIgnored(String path) {
        Ignore ignore = buildIgnoreTree(path);
        return ignore != null && ignore.isIgnored(path);
    }
}
