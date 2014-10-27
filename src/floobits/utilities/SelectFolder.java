package floobits.utilities;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.common.Constants;
import floobits.common.FloorcJson;
import floobits.common.RunLater;
import floobits.common.Settings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;

public class SelectFolder {

    public static void build(String owner, String workspace, final RunLater<String> runLater) {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
        descriptor.setTitle("Select Folder For Workspace");
        descriptor.setDescription("NOTE: Floobits will NOT make a new, root directory inside the folder you choose. If you have cloned the project already, select that folder.");
        FloorcJson floorcJson;
        try {
            floorcJson =  Settings.get();
        } catch (Exception e) {
            Flog.errorMessage("Your floorc.json has invalid json.", null);
            return;
        }
        File file = null;
        String shareDir = floorcJson.share_dir;
        if (shareDir != null) {
            if (shareDir.substring(0, 2).equals("~/")) {
                shareDir = shareDir.replaceFirst("~/", System.getProperty("user.home") + "/");
            }
            file = createDir(shareDir, owner, workspace);
            if (file == null) {
                Flog.errorMessage(String.format("Your floorc.json share_dir setting %s did not work, using default ~/floobits",
                        floorcJson.share_dir), null);
            }
        }
        if (file == null) {
            file = createDir(Constants.shareDir, owner, workspace);
        }
        if (file == null) {
            Flog.errorMessage(String.format("Could not create a directory for this workspace, tried %s", Constants.shareDir), null);
            return;
        }
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
        VirtualFile[] vFiles = FileChooser.chooseFiles(descriptor, null, virtualFile);
        if (vFiles.length == 0) {
            return;
        }
        if (vFiles.length > 1) {
            Flog.errorMessage("You can only select one directory!", null);
            return;
        }
        final String selectedPath = vFiles[0].getPath();
        runLater.run(selectedPath);
    }


    private static File createDir(String path, String owner, String workspace) {
        path = FilenameUtils.concat(path, owner);
        path = FilenameUtils.concat(path, workspace);
        File file = new File(path);
        try {
            FileUtils.forceMkdir(file);
        } catch (IOException e) {
            Flog.warn(e);
            return null;
        }
        return file;
    }
}
