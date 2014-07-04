package floobits;

import com.intellij.openapi.vfs.VirtualFile;
import floobits.common.BinaryBuf;
import floobits.common.OutboundRequestHandler;
import floobits.common.Utils;
import floobits.common.handlers.FlooHandler;
import floobits.utilities.Flog;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;

/**
 * Created by kans on 7/3/14.
 */
public class IntellijBinaryBuf extends BinaryBuf <VirtualFile> {

    public IntellijBinaryBuf(String path, Integer id, byte[] buf, String md5, FlooContext context, OutboundRequestHandler outbound) {
        super(path, id, buf, md5, context, outbound);
    }

    @Override
    public byte[] getBytes() {
        VirtualFile virtualFile = BufHelper.getVirtualFile(context, path);
        if (virtualFile == null) {
            Flog.warn("Couldn't get virtual file in readFromDisk %s", this);
            return null;
        }
        byte[] bytes;
        try {
            bytes = virtualFile.contentsToByteArray();
        } catch (IOException e) {
            Flog.warn("Could not get byte array contents for file %s", this);
            return null;
        }
        return bytes;
    }

    @Override
    public void updateView() {
        VirtualFile virtualFile = BufHelper.getVirtualFile(context, path);
        if (virtualFile == null) {
            virtualFile = createFile();
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

    @Override
    public VirtualFile createFile() {
        return null;
    }

    @Override
    public void send_patch(VirtualFile virtualFile) {
        FlooHandler flooHandler = context.getFlooHandler();
        if (flooHandler == null) {
            return;
        }
        byte[] contents;
        try {
            contents = virtualFile.contentsToByteArray();
        } catch (IOException e) {
            Flog.warn("Couldn't read contents of binary file. %s", virtualFile);
            return;
        }
        String after_md5 = DigestUtils.md5Hex(contents);
        if (md5.equals(after_md5)) {
            Flog.debug("Binary file change event but no change in md5 %s", virtualFile);
            return;
        }
        set(contents, after_md5);
        flooHandler.outbound.setBuf(this);
    }
}
