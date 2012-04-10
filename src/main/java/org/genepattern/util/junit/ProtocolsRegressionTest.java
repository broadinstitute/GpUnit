package org.genepattern.util.junit;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import org.genepattern.gpunit.yaml.Util;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

/**
 * Run all gpunit tests in the ./tests/protocols directory.
 * 
 * @author pcarr
 */
@RunWith(LabelledParameterized.class)
public class ProtocolsRegressionTest extends TestCase {

    private static List<File> initModuleTestFiles() {
        File rootDir = new File("./tests/protocols");
        FileFilter filter = new FileFilter() {
            public boolean accept(File arg0) {
                if (arg0.isDirectory()) {
                    return true;
                }
                String filenameKey = arg0.getName().toLowerCase();
                if (!(filenameKey.endsWith("test.yml") || filenameKey.endsWith("test.yaml"))) {
                    //must end with 'test.yml' or 'test.yaml'
                    return false;
                }
                return true;
            }
        };
        List<File> moduleTestFiles = initModuleTestFiles(rootDir, filter);
        return moduleTestFiles;
    }

    private static List<File> initModuleTestFiles(File rootDir, FileFilter filter) {
        List<File> testFiles = new ArrayList<File>();
        listFilesInto(testFiles, rootDir, filter);
        return testFiles; 
    }

    private static void listFilesInto(List<File> list, File fromDir, FileFilter filter) {
        for(File file : fromDir.listFiles()) {
            if (file.isDirectory()) {
                listFilesInto(list, file, filter);
            }
            else {
                if (filter.accept(file)) {
                    list.add(file);
                }
            }
        }
    }

    /**
     * This parameterized test runs a single unit test for each test case in the Collection of TestData.
     * Each Object[] element of the data array is passed as the arg to the constructor for a new testcase.
     */
    @Parameters
    public static Collection<Object[]> data() {
        List<Object[]> data = new ArrayList<Object[]>();
        List<File> moduleTestFiles = initModuleTestFiles();
        for(File testFile : moduleTestFiles) {
            data.add( new Object[] { testFile } );
        }
        return data;
    }

    private File moduleTestFile;
    public ProtocolsRegressionTest(File moduleTestFile) {
        this.moduleTestFile = moduleTestFile;
    }
    
    @Test
    public void testModule() throws Exception {
        Util.runTest(moduleTestFile);
    }

}
