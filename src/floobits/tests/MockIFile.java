package floobits.tests;

import com.google.gson.Gson;
import floobits.common.interfaces.IFile;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockIFile extends IFile {

    public class MockNode implements Serializable {

        public HashMap<String, MockNode> children;
        public String contents;

        public MockNode(HashMap<String, MockNode> children, String contents) {
            this.children = children;
            this.contents = contents;
        }
    }

    public Boolean isValid = true;
    public Boolean isSpecial = false;
    public Boolean isSymlink = false;
    public Boolean isDirectory = false;
    public String contents;
    public String path;
    public HashMap<String, MockNode> children;
    public int length = 100;

    public static MockNode mockFileFromJSON (String jsonStr) {
        return new Gson().fromJson(jsonStr, (Type) MockNode.class);
    }

    public static MockNode mockFileFromJSON (URL mockFileURL) throws IOException {
        BufferedReader in = new BufferedReader( new InputStreamReader(mockFileURL.openStream()));
        String inputLine;
        StringBuilder b = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            System.out.println(inputLine);
            b.append(inputLine);
        }
        in.close();
        return mockFileFromJSON(b.toString());
    }

    public MockIFile (String path) {
        this.path = path;
    }

    public MockIFile (MockNode mn, String path) {
        if (mn.children != null) {
            isDirectory = true;
            children = mn.children;
        }
        this.contents = mn.contents;
        this.path = path;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean rename(Object obj, String name) {
        return false;
    }

    @Override
    public boolean move(Object obj, IFile d) {
        return false;
    }

    @Override
    public boolean delete(Object obj) {
        return false;
    }

    @Override
    public IFile[] getChildren() {
        if (children == null) {
            return new IFile[0];
        }
        List<IFile> files = new ArrayList<IFile>();
        for (Map.Entry<String, MockNode> entry : children.entrySet()) {
            MockNode value = entry.getValue();
            String name = entry.getKey();
            String filePath = String.format("%s/%s", path, name);
            files.add(new MockIFile(value, filePath));

        }
        IFile[] result = new IFile[files.size()];
        result = files.toArray(result);
        return result;
    }

    @Override
    public String getName() {
        if (path.length() < 1) {
            return "";
        }
        File f = new File(path);
        return f.getName();
    }

    @Override
    public long getLength() {
        if (contents == null) {
            return length;
        }
        return contents.getBytes().length;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return isDirectory;
    }

    @Override
    public boolean isSpecial() {
        return isSpecial;
    }

    @Override
    public boolean isSymLink() {
        return isSymlink;
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
    public byte[] getBytes() {
        if (contents == null) {
            return new byte[0];
        }
        return contents.getBytes();
    }

    @Override
    public boolean setBytes(byte[] bytes) {
        return false;
    }

    @Override
    public void refresh() {

    }

    @Override
    public boolean createDirectories(String dir) {
        return false;
    }

    @Override
    public InputStream getInputStream() {
        if (contents == null) {
            return null;
        }
        return new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
    }
}
