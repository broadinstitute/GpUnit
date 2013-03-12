package org.genepattern.gpunit.yaml;

import java.io.File;

import org.genepattern.gpunit.ModuleTestObject;

public class Util {
    /**
     * Get the basename of the testcase file, only if the extension is 3 or 4 characters.
     * 
     * @param file
     * @return
     */
    static public String dropExtension(File file) {
        if (file==null || file.getName()==null) {
            throw new IllegalArgumentException("file==null");
        }
        
        String name=file.getName();
        int idx=name.lastIndexOf('.');
        if (idx<0) {
            return name;
        }
        int l=name.length();
        int extension_length=(l-idx)-1;
        if (extension_length>4) {
            return name;
        }
        return name.substring(0, idx);
    }
    
    static public String getTestNameFromFile(final File testCaseFile) {
        if (testCaseFile==null) {
            throw new IllegalArgumentException("testCaseFile==null");
        }
        String dirname;
        //by default save output files into a directory based on the test case file
        String basename=Util.dropExtension(testCaseFile);            
        if (testCaseFile.getParentFile() != null) {
            dirname = testCaseFile.getParentFile().getName() + "_" + basename;
        }
        else {
            dirname = basename;
        }
        return dirname;
    }

    static public ModuleTestObject initTestCase(File fromFile) throws Exception {
        if ("gp_execution_log.txt".equals(fromFile.getName().toLowerCase())) {
            return ExecutionLogParser.parse(fromFile);
        }
        ModuleTestObject testCase = ModuleTestParserYaml.parse(fromFile);
        //optionally set the test name based on the fromFile path
        if (testCase.getName() == null) {
            String testName=getTestNameFromFile(fromFile);
            testCase.setName(testName);
        }
        return testCase;
    }
    
}
