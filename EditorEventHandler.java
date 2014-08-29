package floobits.common;

import floobits.common.interfaces.IContext;
import floobits.common.interfaces.IDoc;
import floobits.common.interfaces.IFactory;
import floobits.common.interfaces.IFile;
import floobits.common.protocol.buf.Buf;
import floobits.common.protocol.handlers.FlooHandler;
import floobits.utilities.Flog;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;

public class EditorEventHandler {
    private final IContext context;
    public final FloobitsState state;
    private final OutboundRequestHandler outbound;
    private final InboundRequestHandler inbound;

    public EditorEventHandler(IContext context, FloobitsState state, OutboundRequestHandler outbound, InboundRequestHandler inbound) {
        this.context = context;
        this.state = state;
        this.outbound = outbound;
        this.inbound = inbound;
    }

    public void createFile(final IFile virtualFile) {
        if (context.isIgnored(virtualFile)) {
            return;
        }
        context.setTimeout(100, new Runnable() {
            @Override
            public void run() {
                FlooHandler flooHandler = context.getFlooHandler();
                if (flooHandler == null) {
                    return;
                }
                flooHandler.editorEventHandler.upload(virtualFile);
            }
        });
    }

    public void go() {
        context.listenToEditor(this);
    }

    public void rename(String path, String newPath) {
        if (!state.can("patch")) {
            return;
        }
        Flog.log("Renamed buf: %s - %s", path, newPath);
        Buf buf = state.get_buf_by_path(path);
        if (buf == null) {
            Flog.info("buf does not exist.");
            return;
        }
        String newRelativePath = context.toProjectRelPath(newPath);
        if (newRelativePath == null) {
            Flog.warn(String.format("%s was moved to %s, deleting from workspace.", buf.path, newPath));
            outbound.deleteBuf(buf, true);
            return;
        }
        if (buf.path.equals(newRelativePath)) {
            Flog.info("rename handling workspace rename, aborting.");
            return;
        }
        outbound.renameBuf(buf, newRelativePath);
    }
    
    public void change(IFile file) {
        String filePath = file.getPath();
        if (!state.can("patch")) {
            return;
        }
        if (!context.isShared(filePath)) {
            return;
        }
        final Buf buf = state.get_buf_by_path(filePath);
        if (buf == null) {
            return;
        }
        synchronized (buf) {
            if (Buf.isBad(buf)) {
                Flog.info("buf isn't populated yet %s", file.getPath());
                return;
            }
            buf.send_patch(file);
        }
    }

    public void changeSelection(String path, ArrayList<ArrayList<Integer>> textRanges, boolean following) {
        Buf buf = state.get_buf_by_path(path);
        outbound.highlight(buf, textRanges, false, following);
    }

    public void save(String path) {
        Buf buf = state.get_buf_by_path(path);
        outbound.saveBuf(buf);
    }

    public void softDelete(HashSet<String> files) {
        if (!state.can("patch")) {
            return;
        }

        for (String path : files) {
            Buf buf = state.get_buf_by_path(path);
            if (buf == null) {
                context.warnMessage(String.format("The file, %s, is not in the workspace.", path));
                continue;
            }
            outbound.deleteBuf(buf, false);
        }
    }

    void delete(String path) {
        Buf buf = state.get_buf_by_path(path);
        if (buf == null) {
            return;
        }
        outbound.deleteBuf(buf, true);
    }

    public void deleteDirectory(ArrayList<String> filePaths) {
        if (!state.can("patch")) {
            return;
        }

        for (String filePath : filePaths) {
            delete(filePath);
        }
    }

    public void msg(String chatContents) {
        outbound.message(chatContents);
    }

    public void kick(int userId) {
        outbound.kick(userId);
    }

    public void changePerms(int userId, String[] perms) {
        outbound.setPerms("set", userId, perms);
    }

    public void upload(IFile virtualFile) {
        if (state.readOnly) {
            return;
        }
        if (!virtualFile.isValid()) {
            return;
        }
        String path = virtualFile.getPath();
        Buf b = state.get_buf_by_path(path);
        if (b != null) {
            Flog.info("Already in workspace: %s", path);
            return;
        }
        outbound.createBuf(virtualFile);
    }

    public boolean follow() {
        boolean mode = !state.stalking;
        state.stalking = mode;
        context.statusMessage(String.format("%s follow mode", mode ? "Enabling" : "Disabling"));;
        if (mode && state.lastHighlight != null) {
            inbound._on_highlight(state.lastHighlight);
        }
        return mode;
    }

    public void summon(String path, Integer offset) {
        outbound.summon(path, offset);
    }

    public void sendEditRequest() {
        outbound.requestEdit();
    }

    public void clearHighlights (){
        context.iFactory.clearHighlights();
    }

    public void openChat() {
        Flog.info("Showing user window.");
        context.openChat();
    }
    public void openInBrowser() {
        if(!Desktop.isDesktopSupported()) {
            context.statusMessage("This version of java lacks to support to open your browser.");
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(state.url.toString()));
        } catch (IOException error) {
            Flog.warn(error);
        } catch (URISyntaxException error) {
            Flog.warn(error);
        }
    }

    public void beforeChange(IDoc doc) {
        final IFile virtualFile = doc.getVirtualFile();
        final String path = virtualFile.getPath();
        final Buf bufByPath = state.get_buf_by_path(path);
        if (bufByPath == null) {
            return;
        }
        String msg;
        if (state.readOnly) {
            msg = "This document is readonly because you don't have edit permission in the workspace.";
        } else if (!bufByPath.isPopulated()) {
            msg = "This document is temporarily readonly while we fetch a fresh copy.";
        } else {
            return;
        }
        context.statusMessage(msg);
        doc.setReadOnly(true);
        IFactory.readOnlyBufferIds.add(bufByPath.path);
        final String text = doc.getText();
        context.setTimeout(0, new Runnable() {
            @Override
            public void run() {
                context.writeThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!state.readOnly && bufByPath.isPopulated()) {
                            return;
                        }
                        synchronized (context) {
                            try {
                                context.setListener(false);
                                IDoc d = context.iFactory.getDocument(virtualFile);
                                if (d == null) {
                                    return;
                                }
                                d.setReadOnly(false);
                                d.setText(text);
                                d.setReadOnly(true);
                            } catch (Throwable e) {
                                Flog.warn(e);
                            } finally {
                                context.setListener(true);
                            }
                        }
                    }
                });
            }
        });
    }
}
