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
    static public void runTest(GPClient gpClient, BatchModuleTestObject testObject) throws GpUnitException, FileNotFoundException, AssertionError, Exception {
        runTest(gpClient, null, testObject);
    }
    static public void runTest(GPClient gpClient, BatchProperties batch, BatchModuleTestObject testObject) throws GpUnitException, FileNotFoundException, AssertionError, Exception {
        if (gpClient == null) {
            throw new GpUnitException("gpClient is null");
        }
        if (batch == null) {
            throw new GpUnitException("batch is null");
        }
        if (testObject == null) {
            throw new GpUnitException("testObject is null");
        }
        Throwable initError = testObject.getInitException();
        if (initError != null) {
            throw new Exception(initError.getMessage(), initError);
        }
        if (testObject.getTestCase() == null) {
            throw new Exception("testObject is null");
        }
        
        runTest(gpClient, batch, testObject.getTestCase());
    }
    
//    static private File getJobResultDir(final BatchRun batch, final ModuleTestObject testCase) {
//        File parentDir=batch.getJobResultDir();
//        File downloadDir=new File(parentDir,testCase.getName());
//        return downloadDir;
//    }

    static private void runTest(final GPClient gpClient, final BatchProperties batch, final ModuleTestObject testCase) throws GpUnitException, FileNotFoundException, AssertionError, Exception {
        ModuleRunner runner = new ModuleRunner(testCase);
        
        runner.setGpClient(gpClient);
        runner.runJobAndWait();
        JobResult jobResult = runner.getJobResult();
        
        //optional properties for handling job result files
        //File jobResultDir=batch.getJobResultDir();
        //String rootDownloadDirProp;
        //String batchNameProp;
        //boolean deleteDownloadedResultFiles=true;
        //boolean downloadAndKeepResultFiles=false;
        
        //if (System.getProperty("gpunit.rootDownloadDir") != null) {
        //    validator.setRootDownloadDir(new File(System.getProperty("gpunit.rootDownloadDir")));
        //}
        File jobResultDir=batch.getJobResultDir(testCase, jobResult);
        
        //File jobResultDir=getJobResultDir(batch,testCase);
        JobResultValidator validator = new JobResultValidator(testCase, jobResult, jobResultDir);
        validator.setSaveResultFiles(batch.getSaveDownloads());
        try {
            validator.validate();
        }
        finally {
            if (jobResult != null) {
                validator.clean();
            }
            
            
            //if (isDeleteDownloadedResultFiles()) {
            //    validator.clean();
            //    if (jobResult != null) {
            //        validator.deleteDownloadedResultFiles();
            //    }
            //}
        }
    }

//    static private boolean isDeleteDownloadedResultFiles() {
//        String prop = System.getProperty("gpunit.deleteDownloadedResultFiles", "true");
//        return Boolean.valueOf(prop);
//    }

    /**
     * Get the basename of the testcase file, only if the extension is 3 or 4 characters.
     * 
     * @param file
     * @return
     */
    static private String dropExtension(File file) {
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
    
    static public ModuleTestObject initTestCase(File fromFile) throws Exception {
        if ("gp_execution_log.txt".equals(fromFile.getName().toLowerCase())) {
            return ExecutionLogParser.parse(fromFile);
        }
        ModuleTestObject testCase = ModuleTestParserYaml.parse(fromFile);
        //set the test name
        if (testCase.getName() == null) {
            String testName = "testName";
            
            String basename=dropExtension(fromFile);
            
            if (fromFile.getParentFile() != null) {
                testName = fromFile.getParentFile().getName() + "/" + basename;
            }
            else {
                testName = basename;
            }
            //drop the extension from the name
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
    
//    static public void main(String[] args) {
//        String testFilepath = "./tests/protocols/01_Run/step1/test.yaml";
//        if (args.length > 0) {
//            //first arg is an optional test file
//            testFilepath = args[0];
//        }
//        File testFile = new File(testFilepath);
//        try {
//            System.out.println("starting test "+testFile+" ... ");
//            runTest(testFile);
//            System.out.println("Success!");
//        }
//        catch (Throwable t) {
//            System.err.println("Failure!");
//            t.printStackTrace();
//        }
//    }
}
