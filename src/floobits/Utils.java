package floobits;

import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

class Utils {
    static Timeouts timeouts = Timeouts.create();
    public static Boolean isSharableFile(VirtualFile virtualFile) {
        return (isFile(virtualFile) && virtualFile.exists() && virtualFile.isInLocalFileSystem());
    }
    public static Boolean isFile(VirtualFile virtualFile) {
        return (virtualFile != null  && !(virtualFile.isDirectory() || virtualFile.is(VFileProperty.SPECIAL) ||
                virtualFile.is(VFileProperty.SYMLINK)));
    }
    public static Boolean isSamePath (String p1, String p2) {
        p1 = FilenameUtils.normalizeNoEndSeparator(p1);
        p2 = FilenameUtils.normalizeNoEndSeparator(p2);
        return FilenameUtils.equalsNormalizedOnSystem(p1, p2);
    }

    public static String absPath (String path) {
        return FilenameUtils.concat(Shared.colabDir, path);
    }

    public static String unFuckPath (String path) {
    	return FilenameUtils.normalize(new File(path).getAbsolutePath());
    }

    public static Boolean isShared (String path) {
    	try {
            String unFuckedPath = unFuckPath(path);
            String relativePath = getRelativePath(unFuckedPath, Shared.colabDir, "/");
	        return !relativePath.contains("..");
    	} catch (PathResolutionException e) {
    		return false;
    	} catch (StringIndexOutOfBoundsException e) {
            return false;
        }
    }

    public static String toProjectRelPath (String path) {
        return getRelativePath(path, Shared.colabDir, "/");
    }

    /** 
     * see http://stackoverflow.com/questions/204784/how-to-construct-a-relative-path-in-java-from-two-absolute-paths-or-urls/3054692#3054692
     * Get the relative path from one file to another, specifying the directory separator. 
     * If one of the provided resources does not exist, it is assumed to be a file unless it ends with '/' or
     * '\'.
     * 
     * @param targetPath targetPath is calculated to this file
     * @param basePath basePath is calculated from this file
     * @param pathSeparator directory separator. The platform default is not assumed so that we can test Unix behaviour when running on Windows (for example)
     * @return String
     */
    public static String getRelativePath (String targetPath, String basePath, String pathSeparator) {

        // Normalize the paths
        String normalizedBasePath = FilenameUtils.normalizeNoEndSeparator(basePath);
        String normalizedTargetPath = FilenameUtils.normalizeNoEndSeparator(targetPath);

        // Undo the changes to the separators made by normalization
        if (pathSeparator.equals("/")) {
            normalizedTargetPath = FilenameUtils.separatorsToUnix(normalizedTargetPath);
            normalizedBasePath = FilenameUtils.separatorsToUnix(normalizedBasePath);

        } else if (pathSeparator.equals("\\")) {
            normalizedTargetPath = FilenameUtils.separatorsToWindows(normalizedTargetPath);
            normalizedBasePath = FilenameUtils.separatorsToWindows(normalizedBasePath);

        } else {
            throw new IllegalArgumentException("Unrecognised dir separator '" + pathSeparator + "'");
        }

        String[] base = normalizedBasePath.split(Pattern.quote(pathSeparator));
        String[] target = normalizedTargetPath.split(Pattern.quote(pathSeparator));

        // First get all the common elements. Store them as a string,
        // and also count how many of them there are.
        StringBuilder common = new StringBuilder();

        int commonIndex = 0;
        while (commonIndex < target.length && commonIndex < base.length
                && target[commonIndex].equals(base[commonIndex])) {
            common.append(target[commonIndex] + pathSeparator);
            commonIndex++;
        }

        if (commonIndex == 0) {
            // No single common path element. This most
            // likely indicates differing drive letters, like C: and D:.
            // These paths cannot be relativized.
            throw new PathResolutionException("No common path element found for '" + normalizedTargetPath + "' and '" + normalizedBasePath
                    + "'");
        }   

        // The number of directories we have to backtrack depends on whether the base is a file or a dir
        // For example, the relative path from
        //
        // /foo/bar/baz/gg/ff to /foo/bar/baz
        // 
        // ".." if ff is a file
        // "../.." if ff is a directory
        //
        // The following is a heuristic to figure out if the base refers to a file or dir. It's not perfect, because
        // the resource referred to by this path may not actually exist, but it's the best I can do
        boolean baseIsFile = true;

        File baseResource = new File(normalizedBasePath);

        if (baseResource.exists()) {
            baseIsFile = baseResource.isFile();

        } else if (basePath.endsWith(pathSeparator)) {
            baseIsFile = false;
        }

        StringBuilder relative = new StringBuilder();

        if (base.length != commonIndex) {
            int numDirsUp = baseIsFile ? base.length - commonIndex - 1 : base.length - commonIndex;

            for (int i = 0; i < numDirsUp; i++) {
                relative.append(".." + pathSeparator);
            }
        }
        String commonStr = common.toString();
        // Handle missing trailing slash issues with base project directory:
        if (normalizedTargetPath.equals(commonStr.substring(0, commonStr.length() - 1))) {
            return "";
        }
        relative.append(normalizedTargetPath.substring(commonStr.length()));
        return relative.toString();
    }

    static class PathResolutionException extends RuntimeException {
        PathResolutionException (String msg) {
            super(msg);
        }
    }

    static ArrayList<String> getAllNestedFilePaths(VirtualFile vFile) {
        ArrayList<String> filePaths = new ArrayList<String>();
        if (!vFile.isDirectory()) {
            filePaths.add(vFile.getPath());
            return filePaths;
        }
        for (VirtualFile file : vFile.getChildren()) {
            if (file.isDirectory()) {
                filePaths.addAll(getAllNestedFilePaths(file));
                continue;
            }
            filePaths.add(file.getPath());
        }
        return filePaths;
    }

    static ArrayList<VirtualFile> getAllNestedFiles(VirtualFile vFile, Ignore ignore) {
        ArrayList<VirtualFile> filePaths = new ArrayList<VirtualFile>();
        if (!vFile.isDirectory()) {
            filePaths.add(vFile);
            return filePaths;
        }
        for (VirtualFile file : vFile.getChildren()) {
            if (ignore.isIgnored(file.getPath())) {
                continue;
            }
            if (file.isDirectory()) {
                filePaths.addAll(getAllNestedFiles(file, ignore));
                continue;
            }
            filePaths.add(file);
        }
        return filePaths;
    }

    static void createFile(final VirtualFile virtualFile) {
        Timeout timeout = new Timeout(1000) {
            @Override
            void run(Object... objects) {
                FlooHandler flooHandler = FlooHandler.getInstance();
                if (flooHandler == null) {
                    return;
                }
                flooHandler.upload(virtualFile);
            }
        };
        timeouts.setTimeout(timeout);
    }
}