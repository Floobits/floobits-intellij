package floobits;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.common.RunLater;

public class SelectFolder {

    static void build(final RunLater<String> runLater) {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
        VirtualFile[] vFiles = FileChooser.chooseFiles(descriptor, null, null);
        if (vFiles.length == 0) {
            return;
        }
        runLater.run(vFiles[0].getPath());
    }
}
