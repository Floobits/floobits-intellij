package floobits.common;

import com.intellij.openapi.vfs.VirtualFile;
import floobits.utilities.Flog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

public class Ignore {
    static String[] IGNORE_FILES = {".gitignore", ".hgignore", ".flignore", ".flooignore"};
    // TODO: make this configurable
    static String[] HIDDEN_WHITELIST = {".gitignore", ".hgignore", ".flignore", ".flooignore", ".floo"};

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
    protected Integer size = 0;

    protected HashMap<String, ArrayList<String>> ignores = new HashMap<String, ArrayList<String>>();

    public Ignore (File basePath, Ignore parent, int depth) throws IOException {
        this.file = basePath;
        this.depth = depth;
        this.stringPath = basePath.getPath();
        unfuckedPath = this.file.getPath();
        this.parent = parent;
        this.ignores.put("/TOO_BIG/", new ArrayList<String>());

        Flog.debug("Initializing ignores for %s", this.file);
        for (String name : IGNORE_FILES) {
            name = FilenameUtils.concat(this.file.getPath(), name);
            File ignoreFile = new File(name);
            this.loadIgnore(ignoreFile);
        }
    }

    public Ignore adopt(File f) {
        Ignore child;
        try {
            child = new Ignore(f, this, depth+1);
        } catch (Exception ignored) {
            return null;
        }
        children.put(f.getName(), child);
        return child;
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

    private Boolean isIgnored(String path, String[] splitPath, int depth) {
        String p = "";
        for (int i =0; i<splitPath.length; i++) {
            p = FilenameUtils.concat(p, splitPath[i]);
            if (i <= this.depth) {
                continue;
            }
//            String relPath = Utils.getRelativePath(path, this.file.getPath());
//            File relPathFile = new File(relPath);
            String relPath = Utils.getRelativePath(p, this.file.getPath());
            File relPathFile = new File(p);
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
                            Flog.log("Ignoring %s because %s", path, pattern);
                            return true;
                        }
                        continue;
                    }
                    if (pattern.endsWith("/") && new File(path).isDirectory()) {
                        pattern = pattern.substring(0, pattern.length() - 1);
                    }
                    if (FilenameUtils.wildcardMatch(file_name, pattern)) {
                        Flog.log("Ignoring %s because %s", path, pattern);
                        return true;
                    }
                    if (FilenameUtils.wildcardMatch(relPath, pattern)) {
                        Flog.log("Ignoring %s because %s", path, pattern);
                        return true;
                    }
                }
            }
        }
        depth += 1;
        if (depth > splitPath.length) {
            return false;
        }
        String nextName = splitPath[depth];
        Ignore ignore = this.children.get(nextName);
        if (ignore == null) {
            return false;
        }
        return ignore.isIgnored(path, splitPath, depth);
    }

    public Boolean isIgnored(String path) {
        if (!Utils.isShared(path)) {
            return true;
        }
        path = FilenameUtils.normalizeNoEndSeparator(path);
        if (path.equals(this.stringPath)) {
            return false;
        }
        ArrayList<String> paths = new ArrayList<String>();
        File f = new File(path);
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
        Ignore root;
        try {
            root = new Ignore(new File(Shared.colabDir), null, 0);
        } catch (IOException e) {
            return null;
        }
        LinkedList<Ignore> children = new LinkedList<Ignore>();
        children.add(root);
        while (children.size() > 0) {
            Ignore current = children.pop();
            File[] files = current.file.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    if (!file.isDirectory()) {
                        return false;
                    }
                    if (optionalSparsePath != null && !Utils.isChild(file.getPath(), optionalSparsePath)) {
                        return false;
                    }
                    return true;
                }
            });
            if (files != null){
                for (File file : files) {
    //                this is n2 on the split path since we have to use root :(
                    if (root.isIgnored(file.getPath())) {
                        continue;
                    }
                    Ignore child = current.adopt(file);
                    if (child == null) {
                        continue;
                    }
                    children.push(child);
                }
            }
        }
        return root;
    }

    public static Boolean isIgnored(VirtualFile f) {
        Ignore ignore = buildIgnoreTree(f.getPath());
        return ignore != null && !ignore.isIgnored(f.getPath());
    }
}
