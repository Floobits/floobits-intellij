package floobits;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

public class SelectFolder {

    static void build(String starting_path, final RunLater runLater) {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Please choose a folder.");
        File file = new File(starting_path);
        if (file.exists() && file.isDirectory()){
            fileChooser.setCurrentDirectory(file);
        }
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setApproveButtonText("Select");
        fileChooser.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent arg0) {

                if (arg0.getClickCount() == 2) {
                    File file = fileChooser.getSelectedFile();
                    if (file.isDirectory()) {
                        fileChooser.setCurrentDirectory(file);
                        fileChooser.rescanCurrentDirectory();
                    } else {
                        fileChooser.approveSelection();
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        JFrame frame = new JFrame("FileChooserDemo");
        int retval = fileChooser.showSaveDialog(frame);
        if (retval == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile != null)
                runLater.run(selectedFile);
        }
    }
}
