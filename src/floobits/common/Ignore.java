package floobits.common;

import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.FlooContext;
import floobits.utilities.Flog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.ignore.IgnoreNode;
import org.eclipse.jgit.ignore.IgnoreRule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

public class Ignore {
    static final HashSet<String> IGNORE_FILES = new HashSet<String>(Arrays.asList(".gitignore", ".hgignore", ".flignore", ".flooignore"));
    static final HashSet<String> WHITE_LIST = new HashSet<String>(Arrays.asList(".gitignore", ".hgignore", ".flignore", ".flooignore", ".floo", ".idea"));
    static final ArrayList<String> DEFAULT_IGNORES = new ArrayList<String>(Arrays.asList("extern", "node_modules", "tmp", "vendor", ".idea/workspace.xml", ".idea/misc.xml"));
    static final int MAX_FILE_SIZE = 1024 * 1024 * 5;
    protected final VirtualFile file;
    private final int depth;
    private String rootPath;
    protected final Ignore parent;
    protected final String stringPath;
    protected final HashMap<String, Ignore> children = new HashMap<String, Ignore>();
    protected final ArrayList<VirtualFile> files = new ArrayList<VirtualFile>();
    protected int size = 0;
    private final IgnoreNode ignoreNode = new IgnoreNode();

    public Ignore(VirtualFile virtualFile) {
        this(virtualFile, null, 0, virtualFile.getPath());
    }
    private Ignore (VirtualFile virtualFile, Ignore parent, int depth, String rootPath) {
        this.file = virtualFile;
        this.depth = depth;
        this.rootPath = rootPath;
        this.stringPath = virtualFile.getPath();
        this.parent = parent;
        this.rootPath = rootPath;

        Flog.debug("Initializing ignores for %s", this.file);

        for (VirtualFile vf : file.getChildren()) {
            if (!IGNORE_FILES.contains(vf.getName()) || !vf.isValid()) {
                continue;
            }

            try {
                ignoreNode.parse(vf.getInputStream());
            } catch (IOException e) {
                Flog.warn(e);
            }
        }
        if (depth==0){
            ignoreNode.addRule(new IgnoreRule(".idea/workspace.xml"));
            ignoreNode.addRule(new IgnoreRule(".idea/misc.xml"));
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
    public void recurse() {
        @SuppressWarnings("UnsafeVfsRecursion") VirtualFile[] fileChildren = file.getChildren();
        if (fileChildren == null) {
            return;
        }
        for (VirtualFile file : fileChildren) {
            if (!file.isValid()) {
                continue;
            }
            if (file.is(VFileProperty.SYMLINK) || file.is(VFileProperty.SPECIAL)) {
                continue;
            }
            if (file.getName().startsWith(".") && !WHITE_LIST.contains(file.getName())) {
                continue;
            }
            if (isIgnoredDown(Utils.toProjectRelPath(file.getPath(), rootPath), file.isDirectory())) {
                continue;
            }
            if (file.isDirectory()) {
                Ignore child = new Ignore(file, this, depth + 1, rootPath);
                children.put(file.getName(), child);
                child.recurse();
                size += child.size;
            }

            files.add(file);
            size += file.getLength();
        }
    }

    private boolean isIgnoredUp(String path, boolean isDir, String[] split) {
        IgnoreNode.MatchResult ignored = ignoreNode.isIgnored(path, isDir);
        switch (ignored) {
            case IGNORED:
                return true;
            case NOT_IGNORED:
                return false;
            case CHECK_PARENT:
                break;
        }
        if (split.length <= depth + 1) {
            return false;
        }
        String nextName = split[depth + 1];
        Ignore ignore = children.get(nextName);
        return ignore != null && ignore.isIgnoredUp(path, isDir, split);
    }

    private boolean isIgnoredDown(String path, boolean isDir) {
        IgnoreNode.MatchResult ignored = ignoreNode.isIgnored(path, isDir);
        switch (ignored) {
            case IGNORED:
                return true;
            case NOT_IGNORED:
                return false;
            case CHECK_PARENT:
                break;
        }
        return parent != null && parent.isIgnoredDown(path, isDir);
    }

    private boolean isFlooIgnored(VirtualFile virtualFile, String absPath) {
        if (!Utils.isShared(absPath, rootPath)) {
            Flog.log("Ignoring %s because it isn't shared.", absPath);
            return true;
        }
        if (absPath.equals(stringPath)) {
            return false;
        }
        if (virtualFile.is(VFileProperty.SPECIAL) || virtualFile.is(VFileProperty.SYMLINK)) {
            Flog.log("Ignoring %s because it is special or a symlink.", absPath);
            return true;
        }
        String[] parts = Utils.toProjectRelPath(absPath, rootPath).split("/");
        for (String name : parts) {
            if (name.startsWith(".") && !WHITE_LIST.contains(file.getName())) {
                Flog.log("Ignoring %s because it is hidden.", absPath);
                return true;
            }
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
        if (isFlooIgnored(virtualFile, path))
            return true;

        path =  context.toProjectRelPath(path);
        return isIgnoredUp(path, virtualFile.isDirectory(), path.split("/"));
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
        return IGNORE_FILES.contains(virtualFile.getName()) && virtualFile.isValid();
    }
}
