package floobits.common;

import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.FlooContext;
import floobits.common.jgit.ignore.IgnoreNode;
import floobits.common.jgit.ignore.IgnoreRule;
import floobits.utilities.Flog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Ignore implements Comparable<Ignore>{
    private Ignore root;
    private String rootPath;
    static final HashSet<String> IGNORE_FILES = new HashSet<String>(Arrays.asList(".gitignore", ".hgignore", ".flignore", ".flooignore"));
    static final HashSet<String> WHITE_LIST = new HashSet<String>(Arrays.asList(".gitignore", ".hgignore", ".flignore", ".flooignore", ".floo", ".idea"));
    static final ArrayList<String> DEFAULT_IGNORES = new ArrayList<String>(Arrays.asList("extern", "node_modules", "tmp", "vendor", ".idea/workspace.xml", ".idea/misc.xml"));
    static final int MAX_FILE_SIZE = 1024 * 1024 * 5;
    public final VirtualFile file;
    public final String stringPath;
    public final HashMap<String, Ignore> children = new HashMap<String, Ignore>();
    public final ArrayList<VirtualFile> files = new ArrayList<VirtualFile>();
    public int size = 0;
    private final IgnoreNode ignoreNode = new IgnoreNode();

    public static Ignore BuildIgnore(VirtualFile virtualFile) {
        Ignore ig = new Ignore(virtualFile);
        ig.init(ig);
        // TODO: add more hard-coded ignores
        ig.ignoreNode.addRule(new IgnoreRule(".idea/workspace.xml"));
        ig.ignoreNode.addRule(new IgnoreRule(".idea/misc.xml"));
        ig.ignoreNode.addRule(new IgnoreRule(".git"));
        ig.recurse();
        return ig;
    }

    private Ignore(VirtualFile virtualFile) {
        file = virtualFile;
        stringPath = virtualFile.getPath();
        Flog.debug("Initializing ignores for %s", file);
    }

    public void init(Ignore ig) {
        root = ig;
        rootPath = root.file.getPath();
        for (VirtualFile vf : file.getChildren()) {
            addRules(vf);
        }
    }

    protected void addRules(VirtualFile virtualFile) {
        if (!isIgnoreFile(virtualFile)) {
            return;
        }

        try {
            ignoreNode.parse(virtualFile.getInputStream());
        } catch (IOException e) {
            Flog.warn(e);
        }
    }

    @SuppressWarnings("UnsafeVfsRecursion")
    public void recurse() {
        @SuppressWarnings("UnsafeVfsRecursion") VirtualFile[] fileChildren = file.getChildren();
        if (fileChildren == null) {
            return;
        }
        for (VirtualFile file : fileChildren) {
            if (!file.isValid()) {
                continue;
            }
            String absPath = file.getPath();
            if (isFlooIgnored(file, absPath))  {
                continue;
            }
            String relPath = Utils.toProjectRelPath(absPath, rootPath);

            if (root.isGitIgnored(relPath, file.isDirectory())) {
                continue;
            }

            if (file.isDirectory()) {
                Ignore child = new Ignore(file);
                child.init(root);
                children.put(file.getName(), child);
                child.recurse();
                continue;
            }

            files.add(file);
            size += file.getLength();
        }
    }

    private boolean isGitIgnored(String path, boolean isDir) {
        IgnoreNode.MatchResult ignored = ignoreNode.isIgnored(path, isDir);
        switch (ignored) {
            case IGNORED:
                Flog.log("Ignoring %s because it is ignored by git.", path);
                return true;
            case NOT_IGNORED:
                return false;
            case CHECK_PARENT:
                break;
        }
        String[] split = path.split("/", 2);
        if (split.length != 2) {
            return false;
        }
        String nextName = split[0];
        path = split[1];
        Ignore ignore = children.get(nextName);
        return ignore != null && ignore.isGitIgnored(path, isDir);
    }

    private boolean isFlooIgnored(VirtualFile virtualFile, String absPath) {
        if (virtualFile.is(VFileProperty.SPECIAL) || virtualFile.is(VFileProperty.SYMLINK)) {
            Flog.log("Ignoring %s because it is special or a symlink.", absPath);
            return true;
        }
        if (file.getName().startsWith(".") && !WHITE_LIST.contains(file.getName())) {
            return true;
        }
        if (!virtualFile.isDirectory() && virtualFile.getLength() > MAX_FILE_SIZE) {
            Flog.log("Ignoring %s because it is too big (%s)", absPath, virtualFile.getLength());
            return true;
        }
        return false;
    }

    public Boolean isIgnored(FlooContext context, VirtualFile virtualFile) {

        if (!virtualFile.isValid()){
            Flog.log("Ignoring %s because it is invalid.", virtualFile);
            return true;
        }
        String path = FilenameUtils.separatorsToUnix(virtualFile.getPath());

        if (path.equals(stringPath)) {
            return false;
        }
        if (!Utils.isShared(path, rootPath)) {
            Flog.log("Ignoring %s because it isn't shared.", path);
            return true;
        }
        String[] parts = Utils.toProjectRelPath(path, rootPath).split("/");
        for (String name : parts) {
            if (name.startsWith(".") && !WHITE_LIST.contains(name)) {
                Flog.log("Ignoring %s because it is hidden.", path);
                return true;
            }
        }
        if (isFlooIgnored(virtualFile, path))
            return true;

        path = context.toProjectRelPath(path);
        return isGitIgnored(path, virtualFile.isDirectory());
    }

    public static void writeDefaultIgnores(FlooContext context) {
        Flog.log("Creating default ignores.");
        String path = FilenameUtils.concat(context.colabDir, ".flooignore");

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

    public static boolean isIgnoreFile(VirtualFile virtualFile) {
        return virtualFile != null && IGNORE_FILES.contains(virtualFile.getName()) && virtualFile.isValid();
    }

    @Override
    public int compareTo(@NotNull Ignore ignore) {
        return ignore.size - size;
    }
}
