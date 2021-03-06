package net.cscott.jdoctest;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.tools.shell.Global;

/** JUnit test bridge to re-run the doc tests in the specific javascript file. */
public class JsJUnitTestBridge {
    public final File testFile;
    public JsJUnitTestBridge(File testFile) { this.testFile = testFile; }
    /* stand-alone! */
    public static void main(String[] args) {
        for (String a : args)
            new JsJUnitTestBridge(new File(a)).runDoctest();
    }

    @Test
    public void runDoctest() {
        runDoctest(this.testFile);
    }

    // --- generic implementation, for reuse ---
    public static void runDoctest(String testFile) {
        runDoctest(new File(testFile));
    }

    public static void runDoctest(File testFile) {
        String testText;
        try {
            testText = readFully(testFile);
        } catch (IOException e) {
            fail("Can't read "+testFile);
            return;
        }
        runDoctest(testFile.getPath(), testText);
    }

    public static void runDoctest(String testSource, String testText) {
        // Run each one in turn.
        Context cx = Context.enter();
        try {
            cx.setLanguageVersion(Context.VERSION_1_7); // js 1.7 by default
            boolean expect_fail = Patterns.expectFail(testText);
            Global global = new Global(); // this is also a scope.
            global.init(cx);
            // okay, evaluate the doctest.
            // if the tests fail, we will throw an exception here.
            String fail=null;
            try {
                @SuppressWarnings("unused")
                int testsRun = global.runDoctest(cx, global, testText,
                                                 testSource, 1);
            } catch (AssertionError e) {
                fail = e.getMessage();
                if (fail==null) fail="<unknown assertion failure>";
            } catch (RhinoException e) {
                fail = e.getMessage();
                if (fail==null) fail="<unknown failure>";
            }
            if (expect_fail) {
                fail = (fail!=null) ? null :
                    "Expected to fail, but did not.";
            }
            if (fail!=null)
                fail(testSource+": "+fail);
        } finally {
            Context.exit();
        }
    }

    public static void collectTestsFor(File rootDir, Class<?> testClass,
                                       List<File> results) {
        // XXX implement me
    }

    public static void collectAllTests(File testDir, List<File> results) {
        if (!testDir.isDirectory())
            fail("JDoctest test directory "+testDir+" does not exist");
        for (File f : testDir.listFiles()) {
            if (f.isDirectory())
                collectAllTests(f, results);
            else if (f.getName().endsWith(".js"))
                results.add(f);
        }
    }

    private static String readFully(File f) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[8192];
        Reader r = new InputStreamReader(new FileInputStream(f), "utf-8");
        while (true) {
            int chars = r.read(buf);
            if (chars<0) break;
            sb.append(buf, 0, chars);
        }
        return sb.toString();
    }
}
