package floobits.common;

import com.intellij.openapi.vfs.VirtualFile;
import floobits.FlooContext;
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

    public File file;
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

    public Ignore adopt(File childDirectory) {
        Ignore child;
        try {
            child = new Ignore(childDirectory, this, depth+1);
        } catch (Exception ignored) {
            return null;
        }
        children.put(childDirectory.getName(), child);
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
//                    base_path = Utils.absPath(base_path);
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

    public Boolean isIgnored(FlooContext context, String path) {
        if (!context.isShared(path)) {
            return true;
        }
        path = FilenameUtils.normalizeNoEndSeparator(path);
        if (path.equals(this.stringPath)) {
            return false;
        }
        ArrayList<String> paths = new ArrayList<String>();
        File f = new File(path);
        while (!Utils.isSamePath(f.getPath(), context.colabDir)) {
            paths.add(0, f.getName());
            f = f.getParentFile();
        }
        paths.add(0, context.colabDir);
        return isIgnored(path, paths.toArray(new String[paths.size()]), 0);
    }
}
