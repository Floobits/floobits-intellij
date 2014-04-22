package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.dialogs.SubmitIssueForm;

public class SubmitIssue extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        SubmitIssueForm form = new SubmitIssueForm();
         form.show();
    }
}
