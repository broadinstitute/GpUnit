package org.genepattern.gpunit.yaml;

import java.io.File;
import java.io.FileNotFoundException;

import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.webservice.JobResult;

public class Util {
    final static boolean deleteDownloadedResultFiles = true;

    static public void runTest(File testFile) throws GpUnitException, FileNotFoundException, AssertionError {
        ModuleTestObject testCase = ModuleTestParserYaml.parse(testFile);
        ModuleRunner runner = new ModuleRunner(testCase);
        runner.setGpClient(ModuleRunner.initGpClient());
        runner.run();
        JobResult jobResult = runner.getJobResult();
        JobResultValidator validator = new JobResultValidator(testCase, jobResult);
        try {
            validator.validate();
        }
        finally {
            if (deleteDownloadedResultFiles) {
                if (jobResult != null) {
                    validator.deleteDownloadedResultFiles();
                }
            }
        }
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
