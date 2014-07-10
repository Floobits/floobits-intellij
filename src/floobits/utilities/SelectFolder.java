package floobits.utilities;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.common.Constants;
import floobits.common.RunLater;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;

public class SelectFolder {

    public static void build(String owner, String workspace, final RunLater<String> runLater) {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
        descriptor.setTitle("Select Folder For Workspace");
        descriptor.setDescription("NOTE: Floobits will NOT make a new, root directory inside the folder you choose. If you have cloned the project already, select that folder.");
        String path = FilenameUtils.concat(Constants.shareDir, owner);
        path = FilenameUtils.concat(path, workspace);
        File file = new File(path);
        try {
            FileUtils.forceMkdir(file);
        } catch (IOException e) {
            Flog.warn(e);
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
}
