package org.genepattern.gpunit.yaml;

import java.io.File;
import java.io.FileNotFoundException;

import org.genepattern.client.GPClient;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.webservice.JobResult;

public class Util {
    static public int submitJob(File testFile) throws Exception {
        ModuleTestObject testCase = initTestCase(testFile);
        ModuleRunner runner = new ModuleRunner(testCase);
        GPClient gpClient = ModuleRunner.initGpClient();
        runner.setGpClient(gpClient);
        runner.submitJob();
        return runner.getJobId();
    }

    static public void runTest(File testFile) throws GpUnitException, FileNotFoundException, AssertionError, Exception {
        ModuleTestObject testCase = initTestCase(testFile);
        ModuleRunner runner = new ModuleRunner(testCase);
        GPClient gpClient = ModuleRunner.initGpClient();
        runner.setGpClient(gpClient);
        runner.runJobAndWait();
        JobResult jobResult = runner.getJobResult();
        JobResultValidator validator = new JobResultValidator(testCase, jobResult);
        try {
            validator.validate();
        }
        finally {
            if (isDeleteDownloadedResultFiles()) {
                if (jobResult != null) {
                    validator.deleteDownloadedResultFiles();
                }
            }
        }
    }
    
    static private boolean isDeleteDownloadedResultFiles() {
        String prop = System.getProperty("gpunit.deleteDownloadedResultFiles", "true");
        return Boolean.valueOf(prop);
    }
    
    static public ModuleTestObject initTestCase(File fromFile) throws Exception {
        if ("gp_execution_log.txt".equals(fromFile.getName().toLowerCase())) {
            return ExecutionLogParser.parse(fromFile);
        }
        ModuleTestObject testCase = ModuleTestParserYaml.parse(fromFile);
        return testCase;
    }
    
    static public ModuleRunner initModuleRunner(File fromFile) throws Exception {
        ModuleTestObject testCase = initTestCase(fromFile);
        ModuleRunner runner = new ModuleRunner(testCase);
        runner.setGpClient(ModuleRunner.initGpClient());
        return runner;
    }
    
    static public void main(String[] args) {
        String testFilepath = "./tests/protocols/01_Run/step1/test.yaml";
        if (args.length > 0) {
            //first arg is an optional test file
            testFilepath = args[0];
        }
        File testFile = new File(testFilepath);
        try {
            System.out.println("starting test "+testFile+" ... ");
            runTest(testFile);
            System.out.println("Success!");
        }
        catch (Throwable t) {
            System.err.println("Failure!");
            t.printStackTrace();
        }
    }
}
