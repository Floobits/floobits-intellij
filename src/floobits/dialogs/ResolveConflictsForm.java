package floobits.dialogs;

import javax.swing.*;

public class ResolveConflictsForm {
    private JLabel promptText;
    private JList changedFiles;
    private JPanel contentPanel;
    protected String promptTextFormat = "<html><p>The following remote %s different from your version.</p></html>";
    protected String multiPrompt = "files are";
    protected String singlePrompt = "file is";

    public void setItems(final String[] conflicts) {
        changedFiles.setListData(conflicts);
        String promptFill;
        if (conflicts.length == 1) {
            promptFill = singlePrompt;
        } else {
            promptFill = multiPrompt;
        }
        promptText.setText(String.format(promptTextFormat, promptFill));
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }
}
