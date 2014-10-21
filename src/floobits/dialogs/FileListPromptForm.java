package floobits.dialogs;

import javax.swing.*;

public class FileListPromptForm {
    private JLabel promptText;
    private JList changedFiles;
    private JPanel contentPanel;
    private JList connectionsList;
    private JLabel connectionsText;
    private JPanel connectionsPanel;
    private JScrollPane connectionsScroller;
    protected String promptTextFormat;
    protected String multiPrompt = "files are";
    protected String singlePrompt = "file is";

    public FileListPromptForm(String prompt) {
        promptTextFormat = String.format("<html><p>%s</p></html>", prompt);
        connectionsList.setVisible(false);
        connectionsText.setVisible(false);
        connectionsPanel.setVisible(false);
        connectionsScroller.setVisible(false);
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

    public void setConnections(final String[] connections) {
        connectionsPanel.setVisible(true);
        connectionsText.setVisible(true);
        if (connections.length < 2) {
            connectionsText.setText("There is no one else currently connected to the workspace.");
            return;
        }
        connectionsText.setText(String.format("There are %s connections to the workspace.", connections.length));
        connectionsScroller.setVisible(true);
        connectionsList.setVisible(true);
        connectionsList.setListData(connections);
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }
}
