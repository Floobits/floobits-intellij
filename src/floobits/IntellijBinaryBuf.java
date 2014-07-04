package floobits;

import com.intellij.openapi.vfs.VirtualFile;
import floobits.common.BinaryBuf;
import floobits.common.OutboundRequestHandler;
import floobits.common.Utils;
import floobits.common.handlers.FlooHandler;
import floobits.utilities.Flog;

import java.io.IOException;

public class IntellijBinaryBuf extends BinaryBuf <VirtualFile> {

    public IntellijBinaryBuf(String path, Integer id, byte[] buf, String md5, FlooContext context, OutboundRequestHandler outbound) {
        super(path, id, buf, md5, context, outbound);
    }

    @Override
    protected byte[] getText(VirtualFile f) {
        byte[] bytes;
        try {
            bytes = f.contentsToByteArray();
        } catch (IOException e) {
            Flog.warn("Could not get byte array contents for file %s", this);
            return null;
        }
        return bytes;
    }

    @Override
    public byte[] getText() {
        VirtualFile virtualFile = BufHelper.getVirtualFile(context, path);
        if (virtualFile == null) {
            Flog.warn("Couldn't get virtual file in readFromDisk %s", this);
            return null;
        }
        return getText(virtualFile);
    }

    @Override
    public void updateView() {
        VirtualFile virtualFile = BufHelper.getVirtualFile(context, path);
        if (virtualFile == null) {
            virtualFile = BufHelper.createFile(context, path);
            if (virtualFile == null) {
                Utils.errorMessage("Unable to write file. virtualFile is null.", context.project);
                return;
            }
        }
        FlooHandler flooHandler = context.getFlooHandler();
        if (flooHandler == null) {
            return;
        }
        try {
            Listener.flooDisable();
            virtualFile.setBinaryContent(buf);
        } catch (IOException e) {
            Flog.warn("Writing binary content to disk failed. %s", path);
        } finally {
            Listener.flooEnable();
        }
    }
}
