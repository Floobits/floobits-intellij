package floobits.impl;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import floobits.Listener;
import floobits.common.*;
import floobits.common.interfaces.IContext;
import floobits.common.protocol.FlooUser;
import floobits.dialogs.*;
import floobits.utilities.Flog;
import floobits.windows.ChatManager;

import java.text.NumberFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * I am the link between a project and floobits
 */
public class ContextImpl extends IContext {

    private Listener listener = new Listener(this);
    public Project project;
    public ChatManager chatManager;

    public ContextImpl(Project project) {
        super();
        this.project = project;
        this.iFactory = new FactoryImpl(this, editor);
    }

    public void statusMessage(String message, NotificationType notificationType) {
        Flog.statusMessage(message, notificationType, project);
    }

    @Override
    public void loadChatManager() {
        chatManager = new ChatManager(this);
    }

    @Override public void flashMessage(final String message) {
        Flog.flashMessage(message, project);
    }


    @Override public void warnMessage(String message) {
        Flog.log(message);
        statusMessage(message, NotificationType.WARNING);
        chatStatusMessage(message);
    }

    @Override public void statusMessage(String message) {
        Flog.log(message);
        if (!chatManager.isOpen()) {
            //Only show a status message when chat is not open.
            statusMessage(message, NotificationType.INFORMATION);
        }
        chatStatusMessage(message);
    }

    @Override public void errorMessage(String message) {
        Flog.warn(message);
        statusMessage(message, NotificationType.ERROR);
        chatErrorMessage(message);
    }

    @Override public void chatStatusMessage(String message) {
        if (chatManager != null) {
            chatManager.statusMessage(message);
        }
    }

    @Override public void chatErrorMessage(String message) {
        if (chatManager != null) {
            chatManager.errorMessage(message);
        }
    }

    @Override
    public Object getActualContext() {
        return project;
    }

    @Override
    protected void shareProjectDialog(String name, List<String> orgs, final String host, final boolean _private_, final String projectPath) {
        final ContextImpl context = this;
        ShareProjectDialog shareProjectDialog = new ShareProjectDialog(name, orgs, project, new RunLater<ShareProjectDialog>() {
            @Override
            public void run(ShareProjectDialog dialog) {
                if (API.createWorkspace(host, dialog.getOrgName(), dialog.getWorkspaceName(), context, _private_)) {
                    joinWorkspace(new FlooUrl(host, dialog.getOrgName(), dialog.getWorkspaceName(), Constants.defaultPort, true), projectPath, true);
                }
            }
        });
        shareProjectDialog.createCenterPanel();
        shareProjectDialog.show();
    }

    @Override
    public synchronized void shutdown() {
        super.shutdown();
        if (chatManager != null) {
            chatManager.clearUsers();
        }
        try {
            listener.shutdown();
        } catch (Throwable e) {
            Flog.warn(e);
        }
        listener = new Listener(this);
    }

    @Override
    public void setUsers(Map<Integer, FlooUser> users) {
        if (chatManager == null) {
            return;
        }
        chatManager.setUsers(users);
    }

    public void setListener(boolean b) {
        listener.isListening.set(b);
    }

    @Override
    public void mainThread(Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable);
    }

    @Override
    public void readThread(final Runnable runnable) {
        final ContextImpl context = this;
        mainThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ApplicationManager.getApplication().runReadAction(runnable);
                } catch (Throwable throwable) {
                    API.uploadCrash(context, throwable);
                }
            }
        });
    }

    @Override
    public void writeThread(final Runnable runnable) {
        final long l = System.currentTimeMillis();
        final ContextImpl context = this;
        mainThread(new Runnable() {
            @Override
            public void run() {
                CommandProcessor.getInstance().executeCommand(context.project, new Runnable() {
                    @Override
                    public void run() {
                        ApplicationManager.getApplication().runWriteAction(new Runnable() {
                            @Override
                            public void run() {
                                long time = System.currentTimeMillis() - l;
                                if (time > 200) {
                                    Flog.log("spent %s getting lock", time);
                                }
                                try {
                                    runnable.run();
                                } catch (Throwable throwable) {
                                    API.uploadCrash(context, throwable);
                                }
                            }
                        });
                    }
                }, "Floobits", null);
            }
        });
    }

    @Override
    public void dialog(String title, String body, RunLater<Boolean> runLater) {
        DialogBuilder.build(title, body, runLater);
    }

    @Override
    public void dialogDisconnect(int _tooMuch, int _howMany) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        String howMany = numberFormat.format(_howMany);
        String tooMuch = numberFormat.format(_tooMuch);
        String notice = String.format("You have too many directories that are over %s MB to upload with Floobits.", tooMuch);
        DisconnectNoticeDialog disconnectNoticeDialog = new DisconnectNoticeDialog(new Runnable() {
            @Override
            public void run() {
                shutdown();
            }
        }, String.format("%s We limit it to %d and you have %s big directories.", notice, Constants.TOO_MANY_BIG_DIRS, howMany));
        disconnectNoticeDialog.createCenterPanel();
        disconnectNoticeDialog.show();
    }

    @Override
    public void dialogPermsRequest(final String username, final RunLater<String> runLater) {
        final ContextImpl context = this;
        mainThread(new Runnable() {
            @Override
            public void run() {
                HandleRequestPermsRequestDialog d = new HandleRequestPermsRequestDialog(username, context, runLater);
                d.createCenterPanel();
                d.show();
            }
        });
    }

    @Override
    public boolean dialogTooBig(LinkedList<Ignore> tooBigIgnores) {
        HandleTooBigDialog handleTooBigDialog = new HandleTooBigDialog(tooBigIgnores);
        handleTooBigDialog.createCenterPanel();
        handleTooBigDialog.show();
        return handleTooBigDialog.getExitCode() == 1;
    }

    @Override
    public void dialogResolveConflicts(final Runnable stompLocal, final Runnable stompRemote, final boolean readOnly, final Runnable flee, final String[] conflictedPathsArray) {
        mainThread(new Runnable() {
            @Override
            public void run() {
                ResolveConflictsDialog dialog = new ResolveConflictsDialog(stompLocal, stompRemote, readOnly, flee, conflictedPathsArray);
                dialog.createCenterPanel();
                dialog.show();
            }});
    }

    @Override
    protected String selectAccount(String[] keys) {
        SelectAccount selectAccount = new SelectAccount(project, keys);
        selectAccount.show();
        int exitCode = selectAccount.getExitCode();
        if (exitCode != DialogWrapper.OK_EXIT_CODE) {
            return null;
        }
        return selectAccount.getAccount();
    }


    @Override
    public void chat(String username, String msg, Date messageDate) {
        if (chatManager == null) {
            return;
        }
        if (!chatManager.isOpen()) {
            statusMessage(String.format("%s: %s", username, msg));
        }
        chatManager.chatMessage(username, msg, messageDate);
    }

    @Override
    public void openChat() {
        if (chatManager != null && !chatManager.isOpen()) {
            chatManager.openChat();
        }
    }

    @Override
    public void listenToEditor(EditorEventHandler editorEventHandler) {
        listener.start(editorEventHandler);
    }

    public void setForcedCursorMove() {
        listener.forcedCursorChange.set(true);
    }
}
