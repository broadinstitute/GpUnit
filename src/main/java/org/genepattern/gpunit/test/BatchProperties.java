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
    
    public static boolean isSet(final String val) {
        if (val==null) {
            return false;
        }
        if (val.length()==0) {
            return false;
        }
        return true;
    }
    
    enum GpUnitClient {
        SOAP,
        REST,
    }
    
    // list of properties, configurable via System.setProperty 
    final static public String PROP_GP_URL="genePatternUrl";
    final static public String PROP_GP_USERNAME="username";
    final static public String PROP_GP_PASSWORD="password";
    
    /** type of client, can be 'soap', 'rest', or 'localexec' */
    final static public String PROP_CLIENT="gpunit.client";
    
    /** the location on the local file system for downloading job result files */
    final static public String PROP_OUTPUT_DIR="gpunit.outputdir";
    /** optionally, download job result files into a directory, based on batch.name, e.g.,
     * <pre>
     *  <gpunit.outputdir>/<gpunit.batchname> 
     * </pre>
     */
    final static public String PROP_BATCH_NAME="gpunit.batch.name";
    /**
     * When this is set to true, always download result files to the local file system.
     */
    final static public String PROP_SAVE_DOWNLOADS="gpunit.save.downloads";
    /**
     * When this is set, delete jobs from the server for all succesfully completed test cases.
     */
    final static public String PROP_DELETE_JOBS="gpunit.delete.jobs";

    //
    // for handling file uploads ...
    //
    /**
     * 
     */
    final static public String PROP_UPLOAD_DIR="gpunit.upload.dir";
    final static public String PROP_SERVER_DIR="gpunit.server.dir";
    final static public String PROP_DIFF_DIR="gpunit.diff.dir";
    
    private String gpUrl = "http://127.0.0.1:8080";
    private String gpUsername =  "test";
    private String gpPassword = "test";

    private GpUnitClient client=GpUnitClient.SOAP;

    private String outputDir="./jobResults";
    private String batchName="latest";
    
    private String uploadDir=null;
    private String serverDir=null;
    
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
        String clientStr=System.getProperty(PROP_CLIENT, GpUnitClient.SOAP.toString());
        try {
            client=GpUnitClient.valueOf(clientStr);
        }
        catch (Throwable t) {
            throw new GpUnitException("Error initializing client from "+PROP_CLIENT+"="+clientStr+": "+t.getLocalizedMessage());
        }
        if (System.getProperties().containsKey(PROP_OUTPUT_DIR)) {
            this.outputDir=System.getProperty(PROP_OUTPUT_DIR, outputDir);
        }
        if (System.getProperties().containsKey(PROP_BATCH_NAME)) {
            this.batchName=System.getProperty(PROP_BATCH_NAME, batchName);
        }
        
        //options for handling input files
        this.uploadDir=System.getProperty(PROP_UPLOAD_DIR);
        this.serverDir=System.getProperty(PROP_SERVER_DIR);
        
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
    
    public GpUnitClient getClient() {
        return client;
    }
    
    public boolean getSaveDownloads() {
        return saveDownloads;
    }
    
    public boolean getDeleteJobs() {
        return deleteJobs;
    }
    
    public String getUploadDir() {
        return uploadDir;
    }
    
    public String getServerDir() {
        return serverDir;
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
