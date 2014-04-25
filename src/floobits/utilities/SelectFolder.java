package floobits.utilities;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.common.RunLater;
import floobits.dialogs.DialogBuilder;

public class SelectFolder {

    public static void build(String workspace, final RunLater<String> runLater) {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
        descriptor.setTitle("Select Folder For Workspace");
        descriptor.setDescription("NOTE: Floobits will NOT make a new, root directory inside the folder you choose. If you have cloned the project already, select that folder.");
        VirtualFile[] vFiles = FileChooser.chooseFiles(descriptor, null, null);
        if (vFiles.length == 0) {
            return;
        }
        final String path = vFiles[0].getPath();
        String title = String.format("Confirm path for %s", workspace);
        String body = String.format("Save the workspace files for \"%s\" in \"%s\"?", workspace, path);
        DialogBuilder.build(title, body, new RunLater<Boolean>() {
            public void run(Boolean join) {
                if (!join) {
                    return;
                }
                runLater.run(path);
            }
        });
    }
}
