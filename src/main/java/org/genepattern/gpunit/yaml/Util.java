package org.genepattern.gpunit.yaml;

import java.io.File;
import java.io.FileNotFoundException;

import org.genepattern.client.GPClient;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.gpunit.test.BatchModuleTestObject;
import org.genepattern.gpunit.test.BatchProperties;
import org.genepattern.webservice.JobResult;

public class Util {
    static public void runTest(final GPClient gpClient, final BatchModuleTestObject testObject) throws GpUnitException, FileNotFoundException, AssertionError, Exception {
        runTest(gpClient, null, testObject);
    }
    static public void runTest(final GPClient gpClient, final BatchProperties batchProps, final BatchModuleTestObject testObject) throws GpUnitException, FileNotFoundException, AssertionError, Exception {
        if (gpClient == null) {
            throw new GpUnitException("gpClient is null");
        }
        if (batchProps == null) {
            throw new GpUnitException("batchProps is null");
        }
        if (testObject == null) {
            throw new GpUnitException("testObject is null");
        }
        if (testObject.getTestCase() == null) {
            throw new GpUnitException("testObject.testCase is null");
        }
        Throwable initError = testObject.getInitException();
        if (initError != null) {
            throw new Exception(initError.getMessage(), initError);
        }
        if (testObject.getTestCase() == null) {
            throw new Exception("testObject is null");
        }

        ModuleRunner runner = new ModuleRunner(testObject.getTestCase());
        runner.setGpClient(gpClient);
        runner.setBatchProperties(batchProps);
        runner.runJobAndWait();
        JobResult jobResult = runner.getJobResult();
        
        File jobResultDir=batchProps.getJobResultDir(testObject, jobResult);
        
        JobResultValidator validator = new JobResultValidator(batchProps, testObject, jobResult, jobResultDir);
        validator.setSaveResultFiles(batchProps.getSaveDownloads());
        validator.setDeleteCompletedJobs(batchProps.getDeleteJobs());
        try {
            validator.validate();
        }
        finally {
            if (jobResult != null) {
                validator.clean(runner);
            }
        }
    }

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
    
    static public ModuleRunner initModuleRunner(File fromFile) throws Exception {
        ModuleTestObject testCase = initTestCase(fromFile);
        ModuleRunner runner = new ModuleRunner(testCase);
        runner.setGpClient(ModuleRunner.initGpClient());
        return runner;
    }    
}
