package floobits.tests;

import floobits.common.Ignore;
import floobits.common.Utils;
import floobits.common.interfaces.IFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.*;

public class IgnoreTest {

    protected Ignore ignore;

    @Before
    public void setUp() {
        ignore = Ignore.BuildIgnore(new MockIFile(""));
    }

    @After
    public void tearDown() {
        ignore = null;
    }

    @Test
    public void testCompareTo(){
        Ignore dummyIgnore = Ignore.BuildIgnore(new MockIFile(""));
        ignore.size = 100;
        dummyIgnore.size = 200;
        assertEquals("Ignore's compareTo should return the difference.", ignore.compareTo(dummyIgnore), dummyIgnore.size - ignore.size);
    }

    @Test
    public void testIsFlooIgnored() {
        MockIFile dummy = new MockIFile("");
        assertFalse("Valid file should not be ignored.", ignore.isFlooIgnored(dummy, ""));
        ((MockIFile) ignore.file).isValid = false;
        assertTrue("If a root ignore all files given to it should be ignored.", ignore.isFlooIgnored(dummy, ""));
        dummy.isSpecial = true;
        ((MockIFile) ignore.file).isValid = true;
        assertTrue("If given file is special it should be ignored.", ignore.isFlooIgnored(dummy, ""));
        dummy.isSpecial = false;
        dummy.isSymlink = true;
        assertTrue("If given file is a symlink it should be ignored.", ignore.isFlooIgnored(dummy, ""));
        dummy.isSymlink = false;
        dummy.length = Ignore.MAX_FILE_SIZE + 1;
        assertTrue("If given file is too big it should be ignored.", ignore.isFlooIgnored(dummy, ""));
        dummy.isDirectory = true;
        assertFalse("If given file is a directory it should not be ignored even if it's big.", ignore.isFlooIgnored(dummy, ""));


    }

    @Test
    public void testBuildIgnores() throws IOException {
        URL data = IgnoreTest.class.getResource("ignore_file_test.json");
        assertNotEquals(data, null);
        MockIFile.MockNode mn = MockIFile.mockFileFromJSON(data);
        String basePath = "/foo";
        assertNotEquals(mn, null);
        MockIFile mf = new MockIFile(mn, basePath);
        Ignore i = Ignore.BuildIgnore(mf);
        IFile t1 = new MockIFile(mn.children.get(".git"), ".git");
        assertTrue("Hidden files should be ignored by default.", i.isIgnored(t1, t1.getPath()));
        t1 = new MockIFile(mn.children.get("bar"), "bar");
        assertFalse("Non hidden file should not be ignored by default", i.isIgnored(t1, basePath));
        t1 = new MockIFile(mn.children.get("shouldbeignored"), "shouldbeignored");
        assertTrue("File given in .gitignore file should be ignored.", i.isIgnored(t1, t1.getPath()));
        int count = 0;
        for (IFile file : mf.getChildren()) {
            if (file.getName().equals(".bar")) {
                for (IFile file1 : file.getChildren()) {
                    if (file1.getName().equals("stuff")) {
                        for (IFile file2 : file1.getChildren()) {
                            String path = Utils.toProjectRelPath(file2.getPath(), basePath);
                            if (file2.getName().equals("pepsi.txt")) {
                                assertTrue("Deeply nested ignored file should be ignored", i.isIgnored(file2, path));
                                count++;
                            } else if (file2.getName().equals("coke.txt")) {
                                assertFalse("Deeply nested not-ignored file should not be ignored", i.isIgnored(file2, path));
                                count++;
                            }
                        }
                    }
                }
            }
        }
        assertEquals("Should have run two ignore checks.", 2, count);
    }
}
