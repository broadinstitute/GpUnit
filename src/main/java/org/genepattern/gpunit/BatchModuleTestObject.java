package org.genepattern.gpunit;

import java.io.File;

import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.webservice.JobResult;

/**
 * For running a parameterized test, you need a list of these.
 * The BatchModuleUtil class creates lists of these objects.
 * 
 * @author pcarr
 */
public class BatchModuleTestObject {
    private File testFile;
    private ModuleTestObject testCase;
    private Throwable initException = null;
    
    public void setTestFile(final File f) {
        this.testFile = f;
    }
    public File getTestFile() {
        return testFile;
    }
    
    public void setTestCase(final ModuleTestObject t) {
        this.testCase = t;
    }
    
    public ModuleTestObject getTestCase() {
        return testCase;
    }
    
    public void setInitException(Throwable t) {
        this.initException = t;
    }
    
    public Throwable getInitException() {
        return initException;
    }
    
    public boolean hasInitExceptions() {
        return initException != null;
    }
    
    public String getTestName() {    
        if (testCase != null && testCase.getName() != null) {
            return testCase.getName();
        }
        else if (testFile != null) {
            return testFile.getName();
        }
        else {
            return "";
        }
    }
    
    /**
     * Get a job result directory for the given testCase and jobResult.
     * You download job result files from the server and into this directory.
     *
     * Rule for creating the download directory for a test-case.
     *     if (batch.name is set) {
     *         <gpunit.outputdir>/<gpunit.batch.name>/<testfile.parentdir.name>/<testfile.basename>
     *     }
     *     else {
     *         <gpunit.outputdir>/<testfile.parentdir.name>/<testfile.basename>
     *     }
     *
     * @return a directory into which to download job results for the given completed test
     */
    public File getJobResultDir(final File batchOutputDir, final JobResult jobResult) throws GpUnitException {
        String jobId=null;
        if (jobResult != null && jobResult.getJobNumber()>=0) {
            jobId=""+jobResult.getJobNumber();
        }
        return getJobResultDir(batchOutputDir, jobId);
    }

    /**
     * Get a job result directory for the given testCase and jobId.
     * You download job result files from the server and into this directory.
     *
     * Rule for creating the download directory for a test-case.
     *     if (batch.name is set) {
     *         <gpunit.outputdir>/<gpunit.batch.name>/<testfile.parentdir.name>/<testfile.basename>
     *     }
     *     else {
     *         <gpunit.outputdir>/<testfile.parentdir.name>/<testfile.basename>
     *     }
     *
     * @return a directory into which to download job results for the given completed test
     */
    public File getJobResultDir(final File batchOutputDir, final String jobId) throws GpUnitException {
        File testCaseFile=null;
        String testName=null;
        testCaseFile=getTestFile();
        ModuleTestObject testCase=getTestCase();
        if (testCase != null) {
            testName=testCase.getName();
            if (testName != null && testName.length()==0) {
                testName=null;
            }
        }

        String dirname=null;
        if (testCaseFile != null) {
            dirname=testCase.getTestNameFromFile(testCaseFile);
        }
        else if (testName != null) {
            //otherwise, use the testname
            dirname = testName;
        }
        else if (jobId != null) {
            //otherwise, use the job id
            dirname = jobId;
        }
        else {
            throw new IllegalArgumentException("Can't create job result directory, testCase and jobId are not set!");
        }
        File jobResultDir=new File(batchOutputDir, dirname);
        return jobResultDir;
    }

    public String toString() {
        return getTestName();
    }
    
}
