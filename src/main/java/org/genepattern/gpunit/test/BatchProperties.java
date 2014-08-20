package org.genepattern.gpunit.test;

import java.io.File;

import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.gpunit.yaml.GpUnitFileParser;
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
    
    public enum GpUnitClient {
        SOAP,
        REST,
    }
    
    // list of properties, configurable via System.setProperty 
    final static public String PROP_GP_URL="genePatternUrl";
    final static public String PROP_GP_USERNAME="username";
    final static public String PROP_GP_PASSWORD="password";
    
    /**
     * The root path from which to search for test.yaml files.
     * When this property is set, run all matching *.yaml or *.yml files as gpunit tests.
     */
    public static final String PROP_TESTCASE_DIRS="gpunit.testcase.dirs";

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

    final static public String PROP_UPLOAD_DIR="gpunit.upload.dir";
    final static public String PROP_SERVER_DIR="gpunit.server.dir";
    final static public String PROP_DIFF_DIR="gpunit.diff.dir";
    
    /**
     * Set the 'gpunit.numThreads' system property to override the default number of junit tests to run in parallel.
     */
    public static final String PROP_NUM_THREADS="gpunit.numThreads";

    /**
     * Set the 'gpunit.shutdownTimeout' system property to override the default shutdownTimeout. 
     * The amount of time, in seconds, to wait for all tests to complete. Jobs which have not yet completed 
     * will not be recorded in the junit test report. This should rarely come into play because the 
     * 'gpunit.testTimeout' and/or the 'gpunit.jobCompletionTimeout' will occur first, under normal circumstances.
     * 
     * On shutdowmTimeout, all parallel junit tests are terminated without recording to junit report.
     *  
     */
    public static final String PROP_SHUTDOWN_TIMEOUT="gpunit.shutdownTimeout";

    /**
     * Set the 'gpunit.testTimeout' system property to override the default testTimeout.
     * The total amount of time, in seconds, to wait for each junit test to complete
     * including the time it takes to submit the job to the GP server, to run on the server, 
     * and to validate the results.
     * 
     * On testTimeout, the junit test will fail with a timeout error.
     * 
     */
    public static final String PROP_TEST_TIMEOUT="gpunit.testTimeout";
    
    /**
     * Set the 'gpunit.jobCompletionTimeout' system property to override the default jobCompletionTimeout interval.
     * The amount of time, in seconds, to poll for job completion. This interval is computed from the time the job
     * is submitted (rather than the time it actually starts running).
     * 
     * On jobCompletionTimeout, the junit test will fail with a timeout error.
     * 
     */
    public static final String PROP_JOB_COMPLETION_TIMEOUT="gpunit.jobCompletionTimeout";
    
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
    
    // the amount of time, in seconds, to allow each GenePattern job to run
    private final int jobCompletionTimeout;
    
    // the amount of time, in seconds, to allow each junit test to run
    private final int testTimeout;
    
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
        
        this.testTimeout=initTestTimeout();
        this.jobCompletionTimeout=initJobCompletionTimeout();
    }

    public static int getIntegerProperty(final String propName, int defaultValue) throws GpUnitException {
        if (System.getProperties().containsKey(propName)) {
            String propValue=System.getProperty(propName).trim();
            if (propValue.length()==0) {
                return defaultValue;
            }
            else if (propValue.startsWith("INF")) {
                // Infinity
                return Integer.MAX_VALUE;
            }
            else {
                try {
                    return Integer.parseInt(propValue);
                }
                catch (NumberFormatException e) {
                    throw new GpUnitException("Error parsing "+propName+"="+propValue);
                }
            }
        }
        return defaultValue;
    }
    
    /**
     * Helper method for initializing 'shutdownTimeout' from System properties.
     * If 'shutdownTimeout' is set, use it. 
     * Otherwise, use the greater of testTimeout and jobCompletionTimeout padded by one minute.
     * Otherwise use hard-coded default value of 1500 seconds.
     * 
     * @return
     * @throws GpUnitException
     */
    public static int initShutdownTimeout() throws GpUnitException {
        // if 'shutdownTimeout' is set, use it,
        int shutdownTimeout=BatchProperties.getIntegerProperty(BatchProperties.PROP_SHUTDOWN_TIMEOUT, -1);
        if (shutdownTimeout>0) {
            return shutdownTimeout;
        }
        // otherwise, use the greater of testTimeout and jobCompletionTimeout padded by one minute
        int testTimeout=BatchProperties.getIntegerProperty(BatchProperties.PROP_TEST_TIMEOUT, -1);
        int jobCompletionTimeout=BatchProperties.getIntegerProperty(BatchProperties.PROP_JOB_COMPLETION_TIMEOUT, -1);
        int max = Math.max(testTimeout,  jobCompletionTimeout);
        if (max>0) {
            return 60+max;
        }
        // otherwise use default
        return 1500;
    }
    
    /**
     * Helper method for initializing 'testTimeout' from System properties.
     * If 'testTimeout' is set, use it.
     * Otherwise, if 'jobCompletionTimeout' is set, padded by one minute.
     * Otherwise, if 'shutdownTimeout' is set, use it.
     * Otherwise, use hard-coded default value of 1200 seconds.
     * 
     * @return
     */
    public static int initTestTimeout() throws GpUnitException {
        int testTimeout=BatchProperties.getIntegerProperty(BatchProperties.PROP_TEST_TIMEOUT, -1);
        if (testTimeout>0) {
            return testTimeout;
        }

        int jobCompletionTimeout=BatchProperties.getIntegerProperty(BatchProperties.PROP_JOB_COMPLETION_TIMEOUT, -1);
        if (jobCompletionTimeout>0) {
            return 60+jobCompletionTimeout;
        }

        int shutdownTimeout=BatchProperties.getIntegerProperty(BatchProperties.PROP_SHUTDOWN_TIMEOUT, -1);
        if (shutdownTimeout>0) {
            return shutdownTimeout;
        }

        return 1200;
    }
    
    /**
     * Helper method for initializing 'jobCompletionTimeout' from System properties.
     * If 'jobCompletionTimeout' is set, use it.
     * Otherwise if 'testTimeout' is set, use it.
     * Otherwise if 'shutdownTimeout' is set, use it.
     * Otherwise, use hard-coded value of 900 seconds.
     * 
     * @return
     * @throws GpUnitException
     */
    public static int initJobCompletionTimeout() throws GpUnitException {
        int jobCompletionTimeout=BatchProperties.getIntegerProperty(BatchProperties.PROP_JOB_COMPLETION_TIMEOUT, -1);
        if (jobCompletionTimeout>0) {
            return jobCompletionTimeout;
        }

        int testTimeout=BatchProperties.getIntegerProperty(BatchProperties.PROP_TEST_TIMEOUT, -1);
        if (testTimeout>0) {
            return testTimeout;
        }

        int shutdownTimeout=BatchProperties.getIntegerProperty(BatchProperties.PROP_SHUTDOWN_TIMEOUT, -1);
        if (shutdownTimeout>0) {
            return shutdownTimeout;
        }

        return 900;
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
        String jobId=null;
        if (jobResult != null && jobResult.getJobNumber()>=0) {
            jobId=""+jobResult.getJobNumber();
        }
        return getJobResultDir(batchTestCase, jobId);
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
    public File getJobResultDir(final BatchModuleTestObject batchTestCase, final String jobId) throws GpUnitException {
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
            dirname=GpUnitFileParser.getTestNameFromFile(testCaseFile);
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

    /**
     * @see #PROP_JOB_COMPLETION_TIMEOUT
     */
    public int getJobCompletionTimeout() {
        return jobCompletionTimeout;
    }

    /**
     * @see #PROP_TEST_TIMEOUT
     */
    public int getTestTimeout() {
        return testTimeout;
    }

}
