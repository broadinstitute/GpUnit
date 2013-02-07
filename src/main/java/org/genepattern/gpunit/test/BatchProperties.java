package org.genepattern.gpunit.test;

import java.io.File;

import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.gpunit.yaml.Util;
import org.genepattern.webservice.JobResult;

/**
 * Helper class for setting shared properties of a single run of a batch of gp-unit tests.
 */
public class BatchProperties {
    final static public class Factory {
        static public BatchProperties initFromProps() throws GpUnitException {
            return new BatchProperties();
        }
    }
    
    // list of properties, configurable via System.setProperty 
    final static public String PROP_GP_URL="genePatternUrl";
    final static public String PROP_GP_USERNAME="username";
    final static public String PROP_GP_PASSWORD="password";
    
    final static public String PROP_OUTPUT_DIR="gpunit.outputdir";
    final static public String PROP_BATCH_NAME="gpunit.batch.name";
    final static public String PROP_SAVE_DOWNLOADS="gpunit.save.downloads";
    final static public String PROP_DELETE_JOBS="gpunit.delete.jobs";
    
    private String gpUrl = "http://127.0.0.1:8080";
    private String gpUsername =  "test";
    private String gpPassword = "test";

    private String outputDir="./jobResults";
    private String batchName="latest";
    
    /**
     * By default delete downloaded result files, which also means "only download files when needed".
     * When this property is set to true, gp-unit will download all result files from the GP server
     * into a new directory on the local file system.
     */
    private boolean saveDownloads=false;
    
    /**
     * By default delete the job from the GP server after each successful run of the test.
     */
    private boolean deleteJobs=true;
    
    
    public BatchProperties() throws GpUnitException {
        //initialize values from system properties
        if (System.getProperties().containsKey(PROP_GP_URL)) {
            this.gpUrl=System.getProperty(PROP_GP_URL);
        }
        if (System.getProperties().containsKey(PROP_GP_USERNAME)) {
            this.gpUsername=System.getProperty(PROP_GP_USERNAME);
        }
        if (System.getProperties().containsKey(PROP_GP_PASSWORD)) {
            this.gpPassword=System.getProperty(PROP_GP_PASSWORD);
        }

        if (System.getProperties().containsKey(PROP_OUTPUT_DIR)) {
            this.outputDir=System.getProperty(PROP_OUTPUT_DIR, outputDir);
        }
        if (System.getProperties().containsKey(PROP_BATCH_NAME)) {
            this.batchName=System.getProperty(PROP_BATCH_NAME, batchName);
        }
        //options for handling result files
        if (System.getProperties().containsKey(PROP_SAVE_DOWNLOADS)) {
            this.saveDownloads=Boolean.getBoolean(PROP_SAVE_DOWNLOADS);
        }
        if (System.getProperties().containsKey(PROP_DELETE_JOBS)) {
            this.deleteJobs=Boolean.getBoolean(PROP_DELETE_JOBS);
        }
        this.batchOutputDir=_initBatchOutputDir();
    }
    

    public String getGpUrl() {
        return gpUrl;
    }
    
    public String getGpUsername() {
        return gpUsername;
    }

    public String getGpPassword() {
        return gpPassword;
    }
    
    public boolean getSaveDownloads() {
        return saveDownloads;
    }
    
    public boolean getDeleteJobs() {
        return deleteJobs;
    }
    
    private boolean createdBatchOutputDir=false;
    private File batchOutputDir=null;
    //if necessary, create the top level download directory
    //    for all jobs in the batch
    private File _initBatchOutputDir() throws GpUnitException {
        String path=outputDir;
        if (batchName != null && batchName.length()>0) {
            if (!outputDir.endsWith("/")) {
                path = outputDir + "/" + batchName;
            }
            else {
                path = outputDir + batchName;
            }
        }
        File rval=new File(path);
        if (!rval.exists()) {
            createdBatchOutputDir=rval.mkdirs();
            if (!createdBatchOutputDir) {
                throw new GpUnitException("Failed to initialize parent directory for downloading job results: "+batchOutputDir.getAbsolutePath());
            }
        }
        return rval;
    }
    
    public boolean getCreatedBatchOutputDir() {
        return createdBatchOutputDir;
    }
    
    public File getBatchOutputDir() {
        return batchOutputDir;
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
    public File getJobResultDir(final BatchModuleTestObject batchTestCase, final JobResult jobResult) throws GpUnitException {
        File testCaseFile=null;
        String testName=null;
        if (batchTestCase != null) {
            testCaseFile=batchTestCase.getTestFile();
            ModuleTestObject testCase=batchTestCase.getTestCase();
            if (testCase != null) {
                testName=testCase.getName();
                if (testName != null && testName.length()==0) {
                    testName=null;
                }
            }
        }
        
        String dirname=null;
        if (testCaseFile != null) {
            dirname=Util.getTestNameFromFile(testCaseFile);
        }
        else if (testName != null) {
            //otherwise, use the testname
            dirname = testName;
        }
        else if (jobResult != null) {
            //otherwise, use the job id
            dirname = ""+jobResult.getJobNumber();
        }
        else {
            throw new IllegalArgumentException("Can't create job result directory, testCase and jobResult are not set!");
        }
        File jobResultDir=new File(batchOutputDir, dirname);
        return jobResultDir;
    }

}
