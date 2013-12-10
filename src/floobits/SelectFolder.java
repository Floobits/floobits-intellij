package floobits;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

public class SelectFolder {
    RunLater runLater;

    static void build(String starting_path, final RunLater runLater) {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
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
        fileChooser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                File selectedFile = fileChooser.getSelectedFile();
                runLater.run(selectedFile);
            }
        });
        JFrame frame = new JFrame("FileChooserDemo");
        fileChooser.showOpenDialog(frame);
//        frame.pack();
//        frame.setVisible(true);
    }
}
