package floobits.common;

import floobits.utilities.Flog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class Ignore {
    static String[] IGNORE_FILES = {".gitignore", ".hgignore", ".flignore", ".flooignore"};
    // TODO: make this configurable
    static String[] HIDDEN_WHITELIST = {".gitignore", ".hgignore", ".flignore", ".flooignore", ".floo"};

    //TODO: grab global git ignores:
    static String[] DEFAULT_IGNORES = {"extern", "node_modules", "tmp", "vendor", ".idea/workspace.xml"};
    static int MAX_FILE_SIZE = 1024 * 1024 * 5;

    protected File path;
    protected String unfuckedPath;
    protected Ignore parent;
    protected String stringPath;
    protected ArrayList<Ignore> children = new ArrayList<Ignore>();

    protected ArrayList<File> files = new ArrayList<File>();
    protected Integer size = 0;

    protected HashMap<String, List<String>> ignores = new HashMap<String, List<String>>();

    public Ignore (File basePath, Ignore parent, Boolean recurse) throws IOException{
        this.path = basePath;
        this.stringPath = basePath.getPath();
        unfuckedPath = this.path.getPath();
        this.parent = parent;
        this.ignores.put("/TOO_BIG/", new ArrayList<String>());

        File[] files = this.path.listFiles();

        if (files == null) {
            return;
        }
        for (File file : files)
            try {
                if (FileUtils.isSymlink(file)) {
                    continue;
                }
                if (recurse && file.isDirectory()) {
                    this.children.add(new Ignore(file, this, recurse));
                }
            } catch (Exception ignored) {

            }

        Flog.debug("Initializing ignores for %s", this.path);
        for (String name : IGNORE_FILES) {
            name = FilenameUtils.concat(this.path.getPath(), name);
            File ignoreFile = new File(name);
            this.loadIgnore(ignoreFile);
        }
    }

    public Ignore() throws IOException{
        this(new File(Shared.colabDir), null, false);
    }

    protected void loadIgnore (File file) {
        String ignores[];
        try {
            ignores = FileUtils.readFileToString(file, "UTF-8").split("\\r?\\n");
        } catch (IOException e) {
            return;
        }

        ArrayList igs = new ArrayList<String>();
        for (String ignore: ignores) {
            ignore = ignore.trim();
            if (ignore.length() == 0 || ignore.startsWith("#")) {
                continue;
            }
            igs.add(ignore);
        }

        this.ignores.put(file.getName(), igs);
    }

    public Boolean isIgnored (String path) {
        String relPath;
        if (path.equals(this.stringPath)) {
            return false;
        }

        relPath = Utils.getRelativePath(path, this.path.getPath());

        File relPathFile = new File(relPath);
        for (Entry<String, List<String>> entry : ignores.entrySet()) {
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
                if (pattern.endsWith("/") && new File(path).isDirectory()){
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
        if (parent != null) {
            return parent.isIgnored(path);
        }
        return false;
    }

    public static Boolean isIgnored(String current_path, String abs_path) {
        if (abs_path == null)
            abs_path = current_path;

        if (!Utils.isShared(current_path))
            return true;

        if (Utils.isSamePath(current_path, Shared.colabDir))
            return false;

        File file = new File(current_path);
        File base_path = new File(file.getParent());
        Ignore ignore;
        try {
            ignore = new Ignore(base_path, null, false);
        } catch (IOException e) {
            return false;
        }

        if (ignore.isIgnored(abs_path))
            return true;

        return isIgnored(base_path.getAbsolutePath(), abs_path);
    }

}
