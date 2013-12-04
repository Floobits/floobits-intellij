package floobits;

import java.io.*;
import java.nio.charset.Charset;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.FileUtils;


enum Encoding {
    BASE64("base64"), UTF8("utf8");
    private final String enc;

    Encoding (String enc) {
        this.enc = enc;
    }

    @Override
    public String toString () {
        return this.enc;
    }

    public static Encoding from (String str) {
        for(Encoding v : Encoding.values()) {
            if(v.toString().equals(str)) {
                return v;
            }
        }
        return null;
    }
}

abstract class Buf <T> {
    public String path;
    public File f;
    public Integer id;
    public String md5;
    public T buf;
    public Encoding encoding;

    public Buf (String path, Integer id, T buf, String md5) {
        this.id = id;
        this.path = new String(path);
        this.buf = buf;
        this.md5 = new String(md5);
        try {
            this.f = new File(FilenameUtils.concat(Shared.colabDir, path));
        } catch (NullPointerException e) {

        }

    }

    public String toAbsolutePath () {
        return FilenameUtils.concat(Shared.colabDir, this.path);
    }

    abstract public void readFromDisk () throws IOException;
    abstract public void writeToDisk () throws IOException;
    abstract public void set (String s, String md5);
    protected void update () {
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(this.toAbsolutePath());
        if (virtualFile == null || !virtualFile.exists()) {
            try {
                this.writeToDisk();
            } catch (IOException e) {
                Flog.error(e);
            }
            return;
        }
        final Document d = FileDocumentManager.getInstance().getCachedDocument(virtualFile);
        if (d == null) {
            try {
                this.writeToDisk();
            } catch (IOException e) {
                Flog.error(e);
            }
            return;
        }
        final String string = (String)this.buf;
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    public void run() {
                        d.replaceString(0, d.getTextLength(), string);
                    }
                });
            }
        });
    }

    public static BinaryBuf createBuf (String path, Integer id, byte[] buf, String md5) {
        return new BinaryBuf(path, id, buf, md5);
    }
    public static TextBuf createBuf (String path, Integer id, String buf, String md5) {
        return new TextBuf(path, id, buf, md5);
    }
    public static Buf createBuf (String path, Integer id, Encoding enc, String md5) {
        if (enc == Encoding.BASE64) {
            return new BinaryBuf(path, id, null, md5);
        }
        return new TextBuf(path, id, null, md5);
    }
}

class BinaryBuf extends Buf <byte[]> {

    public BinaryBuf (String path, Integer id, byte[] buf, String md5) {
        super(path, id, buf, md5);
        this.encoding = Encoding.BASE64;
    }

    public void readFromDisk () throws IOException {
        this.buf = FileUtils.readFileToByteArray(this.f);
        this.md5 = DigestUtils.md5Hex(this.buf);
    }

    public void writeToDisk () throws IOException {
        File parent = new File(this.f.getParent());
        parent.mkdirs();
        FileUtils.writeByteArrayToFile(f, this.buf);
    }

    public void set (String s, String md5) {
        this.buf = Base64.decodeBase64(s.getBytes(Charset.forName("UTF-8")));
        this.md5 = new String(md5);
        this.update();
    }
}

class TextBuf extends Buf <String> {

    public TextBuf (String path, Integer id, String buf, String md5) {
        super(path, id, buf, md5);
        this.encoding = Encoding.UTF8;
    }

    public void readFromDisk () throws IOException {
        this.buf = FileUtils.readFileToString(this.f, "UTF-8");
        this.md5 = DigestUtils.md5Hex(this.buf);
    }

    public void writeToDisk () throws IOException {
        File parent = new File(this.f.getParent());
        parent.mkdirs();
        FileUtils.write(f, this.buf, "UTF-8");
    }

    public void set (String s, String md5) {
        this.buf = new String(s);
        this.md5 = new String(md5);
        this.update();
    }
}
