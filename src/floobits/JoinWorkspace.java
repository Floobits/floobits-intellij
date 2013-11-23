package floobits;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.util.text.StringUtil;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JFrame;
import javax.swing.JOptionPane;


public class JoinWorkspace extends AnAction {
    private static Logger Log = Logger.getInstance(Listener.class);

    private static class URLInputVaslidator implements InputValidator {

        @Override
        public boolean canClose(String inputString) {
            return true;
        }

        @Override
        public boolean checkInput(String inputString) {
            try {
                final URL url = new URL(inputString);
                return StringUtil.isNotEmpty(url.getHost());
            }
            catch (MalformedURLException e) {
                return false;
            }
        }
    }


    public void actionPerformed(AnActionEvent e) {
        JFrame frame = new JFrame("JOptionPane showMessageDialog example");
        String inputValue = JOptionPane.showInputDialog("Workspace URL", "https://floobits.com/kansface/asdf");
//        JOptionPane.showMessageDialog(frame, "asdf");
//        JoinWorkspaceDialog dialog = new JoinWorkspaceDialog();
        Log.info(inputValue);
        FloobitsPlugin.joinWorkspace(inputValue);
    }
}
