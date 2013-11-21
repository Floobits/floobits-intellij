import com.intellij.openapi.diagnostic.Logger;
import floobits.FloobitsPlugin;
import floobits.Listener;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created with IntelliJ IDEA.
 * User: kans
 * Date: 11/19/13
 * Time: 7:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class workspace {
    private JTextField workspaceTextField;
    private JPanel panel1;
    private JButton OKButton;
    private JButton cancelButton;
    private static Logger Log = Logger.getInstance(Listener.class);

    public workspace() {
        workspaceTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               Log.info("thins");
               //FloobitsPlugin.joinWorkspace();
            }
        });
    }
}
