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
 * Run a set of gpunit tests, wrapped as junit tests.
 * 
 * @author pcarr
 */
@RunWith(LabelledParameterized.class)
public class ProtocolsRegressionTest extends TestCase {
    
//    private static String[] moduleTestFiles = {
//        "./tests/protocols/step1/test.yaml",
//        "./tests/protocols/step2/test.yaml"
//    };
//    private static File initModuleTestFile(String path) {
//        File file = new File(path);
//        if (file.isAbsolute()) {
//            return file;
//        } 
//        //else, it's a relative path, 
//        //TODO: make relative paths configurable
//        //assume it's relative to the working directory
//        return file;
//    }
//    /**
//     * This parameterized test runs a single unit test for each test case in the Collection of TestData.
//     * Each Object[] element of the data array is passed as the arg to the constructor for a new testcase.
//     */
//    @Parameters
//    public static Collection<Object[]> hardCodeddata() {
//        List<Object[]> data = new ArrayList<Object[]>();
//        //List<String> moduleTestFiles = initModuleTestFiles();
//        //for(String testFile : moduleTestFiles) {
//        //    //data.add( new Object[] { testFile } );
//        //    File file = initModuleTestFile(testFile);
//        //    data.add( new Object[] { file } );
//
//        //}
//        for(String path : moduleTestFiles) {
//            File file = initModuleTestFile(path);
//            data.add( new Object[] { file } );
//        }
//        return data;
//    }
    

//    private static List<String> initModuleTestFiles() {
//        File rootDir = new File("./tests/protocols");
//        FileFilter filter = new FileFilter() {
//            public boolean accept(File arg0) {
//                // TODO Auto-generated method stub
//                if (arg0.isDirectory()) {
//                    return true;
//                }
//                String filenameKey = arg0.getName().toLowerCase();
//                if (!(filenameKey.endsWith("test.yml") || filenameKey.endsWith("test.yaml"))) {
//                    //must end with 'test.yml' or 'test.yaml'
//                    return false;
//                }
//                return true;
//            }
//        };
//        List<String> moduleTestFiles = initModuleTestFiles(rootDir, filter);
//        return moduleTestFiles;
//    }
//
//    private static List<String> initModuleTestFiles(File rootDir, FileFilter filter) {
//        List<String> testFiles = new ArrayList<String>();
//        listFilesInto(testFiles, rootDir, filter);
//        return testFiles; 
//    }
//
//    private static void listFilesInto(List<String> list, File fromDir, FileFilter filter) {
//        for(File file : fromDir.listFiles()) {
//            if (file.isDirectory()) {
//                listFilesInto(list, file, filter);
//            }
//            else {
//                if (filter.accept(file)) {
//                    //File testFile = new File(".", file.getPath());
//                    list.add(file.getPath());
//                }
//            }
//        }
//    }
//
//    /**
//     * This parameterized test runs a single unit test for each test case in the Collection of TestData.
//     * Each Object[] element of the data array is passed as the arg to the constructor for a new testcase.
//     */
//    @Parameters
//    public static Collection<Object[]> data() {
//        List<Object[]> data = new ArrayList<Object[]>();
//        List<String> moduleTestFiles = initModuleTestFiles();
//        for(String testFile : moduleTestFiles) {
//            //data.add( new Object[] { testFile } );
//            //File file = initModuleTestFile(testFile);
//            File file = new File(testFile);
//            data.add( new Object[] { file } );
//
//        }
//        return data;
//    }
//

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
                    //File testFile = new File(".", file.getPath());
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
