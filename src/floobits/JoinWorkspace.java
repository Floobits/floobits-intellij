package floobits;

import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.util.text.StringUtil;


public class JoinWorkspace extends AnAction {

    private static class URLInputValidator implements InputValidator {
        @Override
        public boolean canClose(String inputString) {
            return true;
        }

        @Override
        public boolean checkInput(String inputString) {
            try {
                final URL url = new URL(inputString);
                return StringUtil.isNotEmpty(url.getHost());
            } catch (MalformedURLException e) {
                return false;
            }
        }
    }

    public void actionPerformed(AnActionEvent e) {
        String inputValue = JOptionPane.showInputDialog("Workspace URL", "https://floobits.com/kansface/asdf");
        Flog.info(inputValue);
        FloobitsPlugin.joinWorkspace(inputValue);
    }
}
