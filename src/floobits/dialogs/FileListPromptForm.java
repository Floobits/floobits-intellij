package floobits.dialogs;

import javax.swing.*;

public class FileListPromptForm {
    private JLabel promptText;
    private JList changedFiles;
    private JPanel contentPanel;
    protected String promptTextFormat;
    protected String multiPrompt = "files are";
    protected String singlePrompt = "file is";

    public FileListPromptForm(String prompt) {
        promptTextFormat = String.format("<html><p>%s</p></html>", prompt);
    }

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
