package floobits.dialogs;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class ResolveConflictsDialog extends CustomButtonDialogWrapper {
    protected FileListPromptForm form = new FileListPromptForm("The following remote %s different from your version.");
    protected MessageContainer messageContainer;

    public ResolveConflictsDialog(Runnable stompLocal, Runnable stompRemote, boolean readOnly, Runnable flee,
                                  final String[] conflicts, final String[]connections, int workspaceFileCount,
                                  int projectFilesCount) {
        super(true);
        form.setItems(conflicts);
        form.setConnections(connections);
        CustomButtonAction stompRemoteAction = new CustomButtonAction("Overwrite Remote Files", stompRemote);
        if (readOnly) {
            stompRemoteAction.setEnabled(false);
        }
        actions = new Action[]{
                new CustomButtonAction("Overwrite Local Files", stompLocal),
                stompRemoteAction,
                new CustomButtonAction("Disconnect", flee),
        };
        messageContainer = new MessageContainer(form.getContentPanel(),
                String.format("%d of your project's %d files are currently in the workspace. You can add files via the context menu in your Project view.", workspaceFileCount,
                        projectFilesCount));
        init();
    }

    @Nullable
    @Override
    public JComponent createCenterPanel() {
        return messageContainer.getContentPanel();

    }
}
