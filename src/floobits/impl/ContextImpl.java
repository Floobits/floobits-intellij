package floobits.impl;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.Listener;
import floobits.common.*;
import floobits.common.interfaces.IContext;
import floobits.common.interfaces.IFile;
import floobits.common.protocol.FlooUser;
import floobits.common.protocol.handlers.FlooHandler;
import floobits.dialogs.*;
import floobits.utilities.Colors;
import floobits.utilities.Flog;
import floobits.utilities.IntelliUtils;
import floobits.windows.FloobitsWindowManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * I am the link between a project and floobits
 */
public class ContextImpl extends IContext {

    static public class BalloonState {
        public Image smallGravatar;
        public Image largeGravatar;
        public int lineNumber;
        public Balloon balloon;
    }

    private Listener listener = new Listener(this);
    public ConcurrentHashMap<String, BalloonState> gravatars = new ConcurrentHashMap<String, BalloonState>();
    public Project project;
    public FloobitsWindowManager floobitsWindowManager;
    private ExecutorService pool;

    public ContextImpl(Project project) {
        super();
        this.project = project;
        this.iFactory = new FactoryImpl(this, editor);
    }

    public void statusMessage(String message, NotificationType notificationType) {
        Flog.statusMessage(message, notificationType, project);
    }

    @Override
    public void loadFloobitsWindow() {
        if (floobitsWindowManager == null) {
            floobitsWindowManager = new FloobitsWindowManager(this);
        }
        floobitsWindowManager.createChatWindow(project);
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
        if (floobitsWindowManager != null && !floobitsWindowManager.isOpen()) {
            //Only show a status message when chat is not open.
            statusMessage(message, NotificationType.INFORMATION);
        }
        chatStatusMessage(message);
    }

    @Override public void errorMessage(String message) {
        Flog.error(message);
        statusMessage(message, NotificationType.ERROR);
        chatErrorMessage(message);
    }

    @Override
    public boolean confirmDialog(String message) {
        int answer = JOptionPane.showConfirmDialog(null, message);
        return answer == JOptionPane.YES_OPTION;
    }

    @Override public void chatStatusMessage(String message) {
        if (floobitsWindowManager != null) {
            floobitsWindowManager.statusMessage(message);
        }
    }

    @Override public void chatErrorMessage(String message) {
        if (floobitsWindowManager != null) {
            floobitsWindowManager.errorMessage(message);
        }
    }

    @Override
    public Object getActualContext() {
        return project;
    }

    @Override
    protected void shareProjectDialog(String name, List<String> orgs, final String host, final boolean _private_, final String projectPath) {
        final ContextImpl context = this;
        ShareProjectDialog shareProjectDialog = new ShareProjectDialog(name, orgs, project,
                new RunLater<ShareProjectDialog>() {
                    @Override
                    public void run(ShareProjectDialog dialog) {
                        if (API.createWorkspace(host, dialog.getOrgName(), dialog.getWorkspaceName(), context, _private_)) {
                            FlooUrl url = new FlooUrl(host, dialog.getOrgName(), dialog.getWorkspaceName(), Constants.defaultPort, true);
                            joinWorkspace(url, projectPath, true, null);
                        }
                    }
                },
                new RunLater<ShareProjectDialog>() {
                    @Override
                    public void run(ShareProjectDialog dialog) {
                        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
                        descriptor.setTitle("Select folder to upload");
                        descriptor.setDescription("NOTE: You cannot choose a folder outside of your project.");
                        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(projectPath));
                        VirtualFile[] vFiles = FileChooser.chooseFiles(descriptor, null, virtualFile);
                        if (vFiles.length < 1) {
                            Flog.warn("No directory selected for picking files to upload in share project.");
                            return;
                        }
                        if (API.createWorkspace(host, dialog.getOrgName(), dialog.getWorkspaceName(), context, _private_)) {
                            FlooUrl url = new FlooUrl(host, dialog.getOrgName(), dialog.getWorkspaceName(), Constants.defaultPort, true);
                            String filePath = vFiles[0].getCanonicalPath();
                            if (filePath == null) {
                                Flog.warn("Upload for picked directory in share project has a null path");
                                return;
                            }
                            IFile dirToAdd = iFactory.findFileByIoFile(new File(filePath));
                            joinWorkspace(url, projectPath, true, dirToAdd);
                        }
                    }
                });
        shareProjectDialog.createCenterPanel();
        shareProjectDialog.show();
    }

    @Override
    public void followUser() {
        final FlooHandler flooHandler = this.getFlooHandler();
        if (flooHandler == null) {
            return;
        }
        HashMap<String, Boolean> usersToChoose = new HashMap<String, Boolean>();
        String me = flooHandler.state.username;
        for (FlooUser user : flooHandler.state.users.values()) {
            if (user.username.equals(me)) {
                continue;
            }
            if (user.client.equals("flootty")) {
                continue;
            }
            if (Arrays.asList(user.perms).indexOf("highlight") == -1) {
                continue;
            }
            usersToChoose.put(user.username, flooHandler.state.followedUsers.contains(user.username));
        }
        FollowUserDialog followUserDialog = new FollowUserDialog(usersToChoose, project, new RunLater<FollowUserDialog>() {
            @Override
            public void run(FollowUserDialog dialog) {
                getFlooHandler().state.setFollowedUsers(dialog.getFollowedUsers());
            }
        });
        followUserDialog.createCenterPanel();
        followUserDialog.show();
    }

    @Override
    public void updateFollowing() {
        if (floobitsWindowManager != null) {
            floobitsWindowManager.updateUserList();
        }
    }

    @Override
    public void connected() {
        editor.reset();
        if (pool != null) {
            Flog.info("Pool wasn't null when creating a new one.");
        }
        pool = Executors.newFixedThreadPool(5);
    }

    @Override
    public void removeUser(FlooUser user) {
        statusMessage(String.format("%s left the workspace.", user.username));
        if (floobitsWindowManager != null) {
            floobitsWindowManager.removeUser(user);
        }
        iFactory.removeHighlightsForUser(user.user_id);
    }

    @Override
    public synchronized void shutdown() {
        super.shutdown();
        if (floobitsWindowManager != null) {
            floobitsWindowManager.clearUsers();
        }
        try {
            listener.shutdown();
        } catch (Throwable e) {
            Flog.error(e);
        }
        listener = new Listener(this);
        if (pool != null) {
            pool.shutdownNow();
            pool = null;
        }
    }

    public void setListener(boolean b) {
        listener.isListening.set(b);
    }

    public void setSaving(boolean b) {
        listener.isSaving.set(b);
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
    public boolean dialogTooBig(HashMap<String, Integer> bigStuff) {
        HandleTooBigDialog handleTooBigDialog = new HandleTooBigDialog(bigStuff);
        handleTooBigDialog.createCenterPanel();
        handleTooBigDialog.show();
        return handleTooBigDialog.getExitCode() == 0;
    }

    @Override
    public void dialogResolveConflicts(final Runnable stompLocal, final Runnable stompRemote, final boolean readOnly,
                                       final Runnable flee, final String[] conflictedPathsArray,
                                       final String[] connections) {
        ProjectRootManager prm = ProjectRootManager.getInstance(project);
        final AtomicInteger count = new AtomicInteger();
        count.set(0);
        for (VirtualFile file : prm.getContentRoots()) {
            VfsUtil.iterateChildrenRecursively(file, null, new ContentIterator() {
                @Override
                public boolean processFile(VirtualFile file) {
                    FileImpl fileImpl = new FileImpl(file);
                    if (fileImpl.isValid()) {
                        count.getAndIncrement();
                    }
                    return true;
                }
            });
        }
        mainThread(new Runnable() {
            @Override
            public void run() {
                FlooHandler flooHandler = getFlooHandler();
                if (flooHandler == null) {
                    return;
                }
                ResolveConflictsDialog dialog = new ResolveConflictsDialog(stompLocal, stompRemote, readOnly, flee,
                        conflictedPathsArray, connections, flooHandler.state.numBufs(), count.get());
                dialog.createCenterPanel();
                dialog.show();
            }
        });
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
        if (floobitsWindowManager == null) {
            return;
        }
        if (!floobitsWindowManager.isOpen()) {
            statusMessage(String.format("%s: %s", username, msg));
        }
        floobitsWindowManager.chatMessage(username, msg, messageDate);
    }

    @Override
    public void openFloobitsWindow() {
        if (floobitsWindowManager == null) {
            loadFloobitsWindow();
        }

        if (floobitsWindowManager.isOpen()) {
            return;
        }
        floobitsWindowManager.openFloobitsWindow();
        floobitsWindowManager.updateUserList();
    }

    @Override
    public void closeFloobitsWindow() {
        if (floobitsWindowManager == null) {
            return;
        }
        floobitsWindowManager.closeFloobitsWindow();
    }

    @Override
    public void toggleFloobitsWindow() {
        if (floobitsWindowManager == null) {
            loadFloobitsWindow();
            return;
        }
        floobitsWindowManager.toggleFloobitsWindow();
        if (floobitsWindowManager.isOpen()) {
            floobitsWindowManager.updateUserList();
        }
    }

    @Override
    public void setupFloobitsWindow() {
        if (floobitsWindowManager != null) {
            floobitsWindowManager.clearUsers();
        }
        openFloobitsWindow();
    }

    @Override
    public void listenToEditor(EditorEventHandler editorEventHandler) {
        listener.start(editorEventHandler);
    }

    @Override
    public boolean isAccountAutoGenerated() {
        return IntelliUtils.isAutoGenerated();
    }

    @Override
    public void notifyCompleteSignUp() {
        String url = IntelliUtils.getCompleteSignUpURL(project);
        if (url == null) {
            Flog.log("notifyCompleteSignUp: No pinocchio URL");
            return;
        }
        Flog.info("Complete signup url is:", url);
        chatStatusMessage(String.format("Your account was auto-created. Please %s.",
                Utils.getLinkHTML(url, "click here to complete sign up")));
    }

    @Override
    public void addUser(final FlooUser user) {
        if (pool == null) {
            Flog.info("Pool is null cannot add user.");
            return;
        }

        if (user.color != null) {
            Colors.color_map.put(user.username, user.color);
        }

        statusMessage(String.format("%s joined the workspace on %s (%s).", user.username, user.platform, user.client));
        Flog.info("Adding gravatar for user %s.", user);
        pool.execute(new Runnable() {
            @Override
            public void run() {
                Image img;
                URL url;
                try {
                    url = new URL(user.gravatar);
                } catch (MalformedURLException e) {
                    Flog.info("Could not create url for gravatar %s.", user.gravatar);
                    return;
                }
                try {
                    URLConnection con = url.openConnection();
                    con.setConnectTimeout(10000);
                    con.setReadTimeout(10000);
                    InputStream in = con.getInputStream();
                    img = ImageIO.read(in);
                } catch (IOException e) {
                    Flog.info("Could not load gravatar from network.");
                    return;
                }
                ContextImpl.BalloonState balloonState = new ContextImpl.BalloonState();
                balloonState.largeGravatar = img.getScaledInstance(75, 75, Image.SCALE_SMOOTH);
                balloonState.smallGravatar = img.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
                gravatars.put(user.gravatar, balloonState);
                FlooHandler handler = getFlooHandler();
                if (handler == null) {
                    return;
                }
                if (floobitsWindowManager != null) {
                    floobitsWindowManager.updateUserList();
                }
            }
        });
        if (floobitsWindowManager != null) {
            floobitsWindowManager.addUser(user);
        }
    }
}
