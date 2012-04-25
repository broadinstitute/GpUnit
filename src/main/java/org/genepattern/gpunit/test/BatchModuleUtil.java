package org.genepattern.gpunit.test;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * Utility class for dynamically creating a collection of jUnit tests as a Parameterized Test,
 * by scanning for test case files on the file system.
 * 
 * @author pcarr
 */
public class BatchModuleUtil { 
    final static public String default_root_dir = "./tests";

    final static public Collection<Object[]> data() {
        List<Object[]> data = new ArrayList<Object[]>();
        List<File> testcaseFiles = BatchModuleUtil.findTestcases();
        for(File testFile : testcaseFiles) {
            data.add( new Object[] { testFile } );
        }
        return data;
    }

    final static public Collection<Object[]> data(File testDir) {
        List<Object[]> data = new ArrayList<Object[]>();
        List<File> testcaseFiles = BatchModuleUtil.findTestcases(testDir);
        for(File testFile : testcaseFiles) {
            data.add( new Object[] { testFile } );
        }
        return data;
    }

    final static public FileFilter testcaseFileFilter = new FileFilter() {
        public boolean accept(File arg0) {
            if (arg0.isDirectory()) {
                return true;
            }
            String filenameKey = arg0.getName().toLowerCase();
            if (filenameKey.endsWith("test.yml")) {
                return true;
            }
            if (filenameKey.endsWith("test.yaml")) {
                return true;
            }
            //TODO: 'gp_execution_log.txt'
            //if (filenameKey.equals("gp_execution_log.txt")) {
            //    return true;
            //}
            return false;
            //if (!(filenameKey.endsWith("test.yml") || filenameKey.endsWith("test.yaml"))) {
            //    //must end with 'test.yml' or 'test.yaml'
            //    return false;
            //}
            //return true;
        }
    };

    public static List<File> findTestcases(File rootDir) {
        List<File> testFiles = new ArrayList<File>();
        listFilesInto(testFiles, rootDir, testcaseFileFilter);
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
