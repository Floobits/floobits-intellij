package floobits;

import java.io.File;
import java.util.regex.Pattern;

import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.io.FilenameUtils;

import floobits.Flog;

class Utils {
    public static Boolean isSharableFile(VirtualFile virtualFile) {
        return (virtualFile != null && virtualFile.exists() && virtualFile.isInLocalFileSystem() &&
                !(virtualFile.isDirectory() || virtualFile.isSpecialFile() || virtualFile.isSymLink()));
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
	        return !getRelativePath(unFuckPath(path), Shared.colabDir, "/").contains("..");
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
        String normalizedTargetPath = FilenameUtils.normalizeNoEndSeparator(targetPath);
        String normalizedBasePath = FilenameUtils.normalizeNoEndSeparator(basePath);

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
        relative.append(normalizedTargetPath.substring(common.length()));
        return relative.toString();
    }

    static class PathResolutionException extends RuntimeException {
        PathResolutionException (String msg) {
            super(msg);
        }
    }    
}