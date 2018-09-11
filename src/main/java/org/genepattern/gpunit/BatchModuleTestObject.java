package org.genepattern.gpunit;

import static org.genepattern.gpunit.BatchProperties.isNullOrEmpty;

import java.io.File;

import org.genepattern.gpunit.ModuleTestObject;

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
        String dirname=null;
        if (this.testFile != null) {
            dirname=ModuleTestObject.getJobDirName(this.testFile);
        }
        else if (this.testCase !=null && !isNullOrEmpty(testCase.getName())) {
            //otherwise, use the testname
            dirname = testCase.getName();
        }
        else if (!isNullOrEmpty(jobId)) {
            //otherwise, use the job id
            dirname = jobId;
        }
        else {
            throw new IllegalArgumentException("Can't create job result directory, testCase and jobId are not set!");
        }
        return new File(batchOutputDir, dirname);
    }

    public String toString() {
        return getTestName();
    }
    
}
