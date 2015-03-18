package org.genepattern.gpunit.test;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.gpunit.yaml.GpUnitFileParser;


/**
 * Utility class for dynamically creating a collection of jUnit tests as a Parameterized Test,
 * by scanning for test case files on the file system.
 * 
 * @author pcarr
 */
public class BatchModuleUtil { 
    final static public String default_root_dir = "./tests";

    public static final BatchModuleTestObject initBatchModuleTestObject(final File testFile) {
        ModuleTestObject testCase = null;
        Throwable exception = null;
        try {
            testCase = GpUnitFileParser.initTestCase(testFile);
        }
        catch (Throwable t) {
            exception = t;
        }
        BatchModuleTestObject batchTestObj = new BatchModuleTestObject();
        batchTestObj.setTestFile(testFile);
        batchTestObj.setTestCase(testCase);
        batchTestObj.setInitException(exception);
        return batchTestObj;
    }
    
    /**
     * Initialize the next test-case by parsing the given test file in yaml format.
     * 
     * @param batchIdx
     * @param testFile
     * @return an Object array where,
     *     Object[0] is the batch index,
     *     Object[1] is the test name,
     *     Object[2] is the BatchModuleTestObject
     *     
     */
    public static final Object[] initTestCaseEntry(final int batchIdx, final File testFile) {
        BatchModuleTestObject batchTestObj = initBatchModuleTestObject(testFile);
        return new Object[] { batchIdx, batchTestObj };
    }
    
    final static private Collection<Object[]> dataFromTestcaseFiles(final List<File> testcaseFiles) {
        int batchIdx=-1;
        List<Object[]> data = new ArrayList<Object[]>();
        for(File testFile : testcaseFiles) {
            ++batchIdx;
            Object[] testCaseEntry=initTestCaseEntry(batchIdx, testFile);
            data.add( testCaseEntry );
        }
        return data;
    }

    final static public Collection<Object[]> data() {
        List<File> testcaseFiles = BatchModuleUtil.findTestcases();
        Collection<Object[]> data = dataFromTestcaseFiles(testcaseFiles);
        return data;
    }

    final static public Collection<Object[]> data(final File testDir) {
        List<File> testcaseFiles = BatchModuleUtil.findTestcases(testDir);
        Collection<Object[]> data = dataFromTestcaseFiles(testcaseFiles);
        return data;
    }

    /**
     * Create a new list of test cases from the given list of files.
     * Directories are searched recursively for a set of 0 or more test cases,
     * Files are simply appended to the resulting collection.
     * 
     * @param testCaseFiles
     * @return
     */
    final static public Collection<Object[]> data(final List<File> testDirList) {
        List<File> testcaseFiles = BatchModuleUtil.findTestcases(testDirList);
        Collection<Object[]> data = dataFromTestcaseFiles(testcaseFiles);
        return data;
    }

    /**
     * This filter matches all '*.yml' and '*.yaml' files as well as
     * all 'gp_execution_log.txt' files.
     */
    final static public FileFilter testcaseFileFilter1 = new FileFilter() {
        public boolean accept(File arg0) {
            if (arg0.isDirectory()) {
                return true;
            }
            String filenameKey = arg0.getName().toLowerCase();
            if (filenameKey.endsWith(".yml")) {
                return true;
            }
            if (filenameKey.endsWith(".yaml")) {
                return true;
            }
            if (filenameKey.equals("gp_execution_log.txt")) {
                return true;
            }
            return false;
        }
    };

    public static List<File> findTestcases(List<File> fileset) {
        List<File> testFiles = new ArrayList<File>();
        for(File file : fileset) {
            if (file.isFile()) {
                if (testcaseFileFilter1.accept(file)) {
                    testFiles.add(file);
                }
                else {
                    //TODO: better logging, or throw an exception
                    System.err.println("Warning: Ignoring testcase file: "+file.getAbsolutePath());
                }
            }
            else if (file.isDirectory()) {
                listFilesInto(testFiles, file, testcaseFileFilter1);
            }
        }
        return testFiles;
    }
    public static List<File> findTestcases(File rootDir) {
        List<File> testFiles = new ArrayList<File>();
        listFilesInto(testFiles, rootDir, testcaseFileFilter1);
        return testFiles; 
    }
    private static void listFilesInto(List<File> list, File fromDir, FileFilter filter) {
        if (!fromDir.exists()) {
            System.err.println("'test.dir' doesn't exist: "+fromDir.getAbsolutePath());
            return;
        }
        if (!fromDir.canRead()) {
            System.err.println("'test.dir' cannot be read: "+fromDir.getAbsolutePath());
            return;
        }
        
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
     * Load a list of root directories from System.properties.
     * 
     * @return
     */
    private static List<File> initFileset() {
        List<File> fileset = new ArrayList<File>();

        String rootDirProp =  System.getProperty("test.dir", default_root_dir);
        if (rootDirProp == null || rootDirProp.length() == 0) {
            System.err.println("Missing required system property, 'test.dir'");
            return fileset;
        }
        File rootDir = new File(rootDirProp);
        fileset.add(rootDir);
        return fileset;
    }
    
    public static List<File> findTestcases() {
        List<File> testCases = new ArrayList<File>();
        List<File> rootFileset = initFileset();
        
        for(final File rootDir : rootFileset) {
            final List<File> moduleTestFiles = findTestcases(rootDir);
            testCases.addAll(moduleTestFiles);
        }
        return testCases;
    }
}
