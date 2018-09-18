package org.genepattern.gpunit;

import java.io.File;
import java.util.Properties;

import org.genepattern.gpunit.GpUnitException;

/**
 * Helper class for setting shared properties of a single run of a batch of gp-unit tests.
 */
public class BatchProperties {
    public static final boolean isNullOrEmpty(final String str) {
        return str==null || str.trim().length()==0;
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

//    this code snippet is not needed when the 'log4j.properties' file is on the classpath
//    protected static void setIfNotSet(final String key, final String value) {
//        System.setProperty(key, System.getProperty(key, value));
//    }
//    protected static void initLogging() {
//        setIfNotSet("org.apache.commons.logging.Log", 
//            "org.apache.commons.logging.impl.SimpleLog" );
//        setIfNotSet("org.apache.commons.logging.simplelog.showdatetime", 
//            "true" );
//        setIfNotSet("org.apache.commons.logging.simplelog.log.org.apache.http", 
//            "ERROR" );
//        setIfNotSet("org.apache.commons.logging.simplelog.log.org.apache.http.wire", 
//            "ERROR" ); 
//
//        // to debug mystery 'System.out' file in working dir ...
//        // java ... -Dlog4j.debug ...
//        // setIfNotSet("log4j.debug", "true" ); 
//    }
//    static {
//        initLogging();
//    }

    /**
     * set 'gp.url' to the server on which to run the tests, e.g.
     *   -Dgp.url=https://genepattern.broadinstitute.org
     *   -Dgp.url=http://127.0.0.1:8080
     */
    final static public String PROP_GP_URL="gp.url";

    /**
     * set 'gp.user' to a valid server username
     */
    final static public String PROP_GP_USERNAME="gp.user";

    /**
     * optionally set 'gp.password' to a valid password
     */
    final static public String PROP_GP_PASSWORD="gp.password";
    
    /**
     * The root path from which to search for test.yaml files.
     * When this property is set, run all matching *.yaml or *.yml files as gpunit tests.
     */
    public static final String PROP_TESTCASE_DIRS="gpunit.testcase.dirs";
    
    /** the location on the local file system for downloading job result files */
    final static public String PROP_OUTPUT_DIR="gpunit.outputdir";
    /** optionally, download job result files into a directory, based on batch.name, e.g.,
     * <pre>
     *  <gpunit.outputdir>/<gpunit.batch.name> 
     * </pre>
     */
    final static public String PROP_BATCH_NAME="gpunit.batch.name";
    /**
     * When this is set to true, always download result files to the local file system.
     */
    final static public String PROP_SAVE_DOWNLOADS="gpunit.save.downloads";
    /**
     * set 'gpunit.save.job.json' to true when you want to save the {jobId}.json 
     * output from the rest api (for debugging)
     */
    final static public String PROP_SAVE_JOB_JSON="gpunit.save.job.json";
    /**
     * When this is set, delete jobs from the server for all successfully completed test cases.
     */
    final static public String PROP_DELETE_JOBS="gpunit.delete.jobs";

    final static public String PROP_UPLOAD_DIR="gpunit.upload.dir";
    final static public String PROP_SERVER_DIR="gpunit.server.dir";
    
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

    /**
     * Set the 'gpunit.localAssertions' system property to override the default (true).
     * When true, tests are validated local (on the gpunit client machine) by downloading
     * result files from the GP server if necessary.
     * 
     */
    private static final String PROP_LOCAL_ASSERTIONS = "gpunit.localAssertions"; // set to true to force local diffs for backward compatibility

    /**
     * Set 'gpunit.testData' to change the default location for input files.
     * By default, gpunit.testData=ftp://gpftp.broadinstitute.org
     * Example entry in test.yaml file:
       <pre>
           input.file: "<%gpunit.testData%>/gpunit/FileDifference/input/TextFile.txt"
       </pre>
     */
    public static final String PROP_TEST_DATA="gpunit.testData";

    private String gpUrl = "http://127.0.0.1:8080";
    private String gpUsername =  "test";
    private String gpPassword = "test";

    private final String outputDir; //default: "./jobResults";
    private final String batchName; //default: "latest";
    
    private String uploadDir=null;
    private String serverDir=null;
    
    /**
     * By default delete downloaded result files, which also means "only download files when needed".
     * When this property is set to true, gp-unit will download all result files from the GP server
     * into a new directory on the local file system.
     */
    private boolean saveDownloads=false;
    
    private final boolean saveJobJson; //default: false;

    /**
     * By default delete the job from the GP server after each successful run of the test.
     */
    private boolean deleteJobs=true;
    
    // the amount of time, in seconds, to allow each GenePattern job to run
    private final int jobCompletionTimeout;
    
    // the amount of time, in seconds, to allow each junit test to run
    private final int testTimeout;
    
    // force GpUnit to use local assertions/diffs instead of server diffs; for backward compatibility
    private boolean localAssertions = true;

    public BatchProperties(final Properties sysProps) throws GpUnitException {
        //initialize values from system properties
        if (sysProps.containsKey(PROP_GP_URL)) {
            this.gpUrl=sysProps.getProperty(PROP_GP_URL);
        }
        if (sysProps.containsKey(PROP_GP_USERNAME)) {
            this.gpUsername=sysProps.getProperty(PROP_GP_USERNAME);
        }
        if (sysProps.containsKey(PROP_GP_PASSWORD)) {
            this.gpPassword=sysProps.getProperty(PROP_GP_PASSWORD);
        }
        if (sysProps.containsKey(PROP_OUTPUT_DIR)) {
            this.outputDir=sysProps.getProperty(PROP_OUTPUT_DIR, "./jobResults");
        }
        else {
            this.outputDir="./jobResults";
        }
        if (sysProps.containsKey(PROP_BATCH_NAME)) {
            this.batchName=sysProps.getProperty(PROP_BATCH_NAME, "latest");
        }
        else {
            this.batchName="latest";
        }
        
        //options for handling input files
        this.uploadDir=sysProps.getProperty(PROP_UPLOAD_DIR);
        this.serverDir=sysProps.getProperty(PROP_SERVER_DIR);
        
        //options for handling result files
        if (sysProps.containsKey(PROP_SAVE_DOWNLOADS)) {
            //this.saveDownloads=Boolean.getBoolean(PROP_SAVE_DOWNLOADS);
            this.saveDownloads=Boolean.valueOf(sysProps.getProperty(PROP_SAVE_DOWNLOADS));
        }
        this.saveJobJson=Boolean.valueOf(sysProps.getProperty(PROP_SAVE_JOB_JSON, "false"));
        if (sysProps.containsKey(PROP_DELETE_JOBS)) {
            //this.deleteJobs=Boolean.getBoolean(PROP_DELETE_JOBS);
            this.deleteJobs=Boolean.valueOf(sysProps.getProperty(PROP_DELETE_JOBS));
        }
        this.batchOutputDir=initDir(outputDir, batchName);
        this.createdBatchOutputDir=mkdirIfNecessary(batchOutputDir);
        
        if (sysProps.containsKey(PROP_LOCAL_ASSERTIONS)) {
            //this.localAssertions=Boolean.getBoolean(PROP_LOCAL_ASSERTIONS);
            this.localAssertions=Boolean.valueOf(sysProps.getProperty(PROP_LOCAL_ASSERTIONS));
        }
        this.testTimeout=initTestTimeout(sysProps);
        this.jobCompletionTimeout=initJobCompletionTimeout(sysProps);
    }
    
    public BatchProperties(final Builder in) throws GpUnitException {
        //initialize values from Builder
        this.gpUrl=in.scheme+"://"+in.host+in.initPort();
        this.gpUsername=in.username;
        this.gpPassword=in.password;
        this.outputDir=in.outputdir;
        this.batchName=in.batchName;
        this.uploadDir=in.uploadDir;
        this.serverDir=in.serverDir;
        this.saveDownloads=in.saveDownloads;
        this.saveJobJson=in.saveJobJson;
        this.deleteJobs=in.deleteJobs;
        this.batchOutputDir=initDir(outputDir, batchName);
        this.createdBatchOutputDir=mkdirIfNecessary(batchOutputDir);
        this.localAssertions=in.localAssertions;
        this.testTimeout=in.initTestTimeout();
        this.jobCompletionTimeout=in.initJobCompletionTimeout();
    }

    public static int getIntegerProperty(final Properties sysProps, final String propName, int defaultValue) throws GpUnitException {
        if (sysProps.containsKey(propName)) {
            String propValue=sysProps.getProperty(propName).trim();
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
     * Helper for retrieving substitution values for property references in yaml files.
     * @param propName
     * @return
     */
    public String getSubstitutionProperty(final String propName) throws GpUnitException {
        //TODO: replace with Properties arg
        if (System.getProperties().containsKey(propName)) {
            return System.getProperty(propName).trim();
        }
        else {
            throw new GpUnitException("Reference to undefined property: " + propName);
        }
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
    public static int initShutdownTimeout(final Properties sysProps) throws GpUnitException {
        // if 'shutdownTimeout' is set, use it,
        int shutdownTimeout=BatchProperties.getIntegerProperty(sysProps, BatchProperties.PROP_SHUTDOWN_TIMEOUT, -1);
        if (shutdownTimeout>0) {
            return shutdownTimeout;
        }
        // otherwise, use the greater of testTimeout and jobCompletionTimeout padded by one minute
        int testTimeout=BatchProperties.getIntegerProperty(sysProps, BatchProperties.PROP_TEST_TIMEOUT, -1);
        int jobCompletionTimeout=BatchProperties.getIntegerProperty(sysProps, BatchProperties.PROP_JOB_COMPLETION_TIMEOUT, -1);
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
    protected static int initTestTimeout(final Properties sysProps) throws GpUnitException {
        int testTimeout=BatchProperties.getIntegerProperty(sysProps, BatchProperties.PROP_TEST_TIMEOUT, -1);
        if (testTimeout>0) {
            return testTimeout;
        }

        int jobCompletionTimeout=BatchProperties.getIntegerProperty(sysProps, BatchProperties.PROP_JOB_COMPLETION_TIMEOUT, -1);
        if (jobCompletionTimeout>0) {
            return 60+jobCompletionTimeout;
        }

        int shutdownTimeout=BatchProperties.getIntegerProperty(sysProps, BatchProperties.PROP_SHUTDOWN_TIMEOUT, -1);
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
    protected static int initJobCompletionTimeout(final Properties sysProps) throws GpUnitException {
        int jobCompletionTimeout=getIntegerProperty(sysProps, BatchProperties.PROP_JOB_COMPLETION_TIMEOUT, -1);
        if (jobCompletionTimeout>0) {
            return jobCompletionTimeout;
        }

        int testTimeout=getIntegerProperty(sysProps, BatchProperties.PROP_TEST_TIMEOUT, -1);
        if (testTimeout>0) {
            return testTimeout;
        }

        int shutdownTimeout=getIntegerProperty(sysProps, BatchProperties.PROP_SHUTDOWN_TIMEOUT, -1);
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
    
    public boolean getSaveDownloads() {
        return saveDownloads;
    }
    
    public boolean getSaveJobJson() {
        return saveJobJson;
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
    
    public boolean getRunLocalAssertions() {
        return localAssertions;
    }

    private final boolean createdBatchOutputDir;
    private final File batchOutputDir;

    /**
     * Initialize a directory path. 
     * Handle special cases:
     *   (1) batch.name not set
     *   (2) gpunit.outputdir with and without file separator
     * 
     * Use this to create the top level download directory for all jobs in the batch.
     * Template:
     *     <gpunit.outputdir>[/<gpunit.batch.name>]
     * Default:
     *     ./jobResults/latest
     * 
     * @param parentDir - the <gpunit.outputdir>, default: "./jobResults"
     * @param name - <gpunit.batch.name>, default: "latest"
     * @return a new File object
     * @throws GpUnitException
     */
    private static File initDir(final String parentDir, final String name) throws GpUnitException {
        final String path;
        if (name != null && name.length()>0) {
            if (!parentDir.endsWith("/")) {
                path=parentDir + "/" + name;
            }
            else {
                path=parentDir + name;
            }
        }
        else {
            path=parentDir;
        } 
        return new File(path);
    }
    
    private static boolean mkdirIfNecessary(final File batchOutputDir) throws GpUnitException {
        if (!batchOutputDir.exists()) {
            boolean createdBatchOutputDir=batchOutputDir.mkdirs();
            if (!createdBatchOutputDir) {
                throw new GpUnitException("Failed to initialize parent directory for downloading job results: "+batchOutputDir.getAbsolutePath());
            }
            return createdBatchOutputDir;
        }
        return false;
    }
    
    public boolean getCreatedBatchOutputDir() {
        return createdBatchOutputDir;
    }
    
    public File getBatchOutputDir() {
        return batchOutputDir;
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

    /**
     * Initialize BatchProperties from System properties
     */
    public static final BatchProperties initFromProps() throws GpUnitException {
        Properties props;
        try {
            props=System.getProperties();
        }
        catch (SecurityException e) {
            props=new Properties();
        }
        catch (Throwable t) {
            props=new Properties();
        }
        return new BatchProperties(props);
    }

    public static final class Builder {
        //private String gpUrl = "http://127.0.0.1:8080";
        private String scheme="http";
        private String host="127.0.0.1";
        //private String port=":8080";
        private String port="";
        //private String servletPath="/gp";
        private String username =  "test";
        private String password = "test";
        
        private String outputdir="./jobResults";
        private String batchName="latest";
        private String uploadDir=null;
        private String serverDir=null;
        private boolean saveDownloads=false;
        private boolean saveJobJson=false;
        private boolean localAssertions = true;

        private int testTimeout = -1;
        private int jobCompletionTimeout = -1;
        private int shutdownTimeout = -1;

        private boolean deleteJobs = true;
        
        public Builder scheme(final String scheme) {
            this.scheme=scheme;
            return this;
        }
        
        public Builder host(final String host) {
            this.host=host;
            return this;
        }
        
        public Builder port(final Integer port) {
            if (port==null) {
                this.port="";
            }
            else if (port <= 0) {
                throw new IllegalArgumentException("Must be null or a valid port number, port="+port);
            }
            else {
                this.port=":"+port;
            }
            return this;
        }
        
//        public Builder servletPath(final String servletPath) {
//            if (isNullOrEmpty(servletPath)) {
//                throw new IllegalArgumentException("Invalid servletPath='"+servletPath+"'");
//            }
//            if (servletPath.startsWith("/")) {
//                this.servletPath=servletPath;
//            }
//            else {
//                this.servletPath="/"+servletPath;
//            }
//            return this;
//        }
        
        public Builder username(final String username) {
            this.username=username;
            return this;
        }
        
        public Builder password(final String password) {
            this.password=password;
            return this;
        }

        // gpunit.outputdir
        public Builder outputdir(final String outputdir) {
            this.outputdir=outputdir;
            return this;
        }
        
        // gpunit.batch.name
        public Builder batchName(final String batchName) {
            this.batchName=batchName;
            return this;
        }
        
        // gpunit.upload.dir
        public Builder uploadDir(final String uploadDir) {
            this.uploadDir=uploadDir;
            return this;
        }
        
        // gpunit.server.dir
        public Builder serverDir(final String serverDir) {
            this.serverDir=serverDir;
            return this;
        }
        
        // gpunit.save.downloads
        public Builder saveDownloads(final boolean saveDownloads) {
            this.saveDownloads=saveDownloads;
            return this;
        }

        // gpunit.save.job.json
        public Builder saveJobJson(final boolean saveJobJson) {
            this.saveJobJson=saveJobJson;
            return this;
        }

        // gpunit.delete.jobs
        public Builder deleteJobs(final boolean deleteJobs) {
            this.deleteJobs=deleteJobs;
            return this;
        }
        
        // gpunit.localAssertions
        public Builder localAssertions(final boolean localAssertions) {
            this.localAssertions=localAssertions;
            return this;
        }

        // gpunit.testTimeout
        public Builder testTimeout(final int testTimeout) {
            this.testTimeout=testTimeout;
            return this;
        }
        
        // gpunit.jobCompletionTimeout
        public Builder jobCompletionTimeout(final int jobCompletionTimeout) {
            this.jobCompletionTimeout=jobCompletionTimeout;
            return this;
        }
        
        // gpunit.shutdownTimeout
        public Builder shutdownTimeout(final int shutdownTimeout) {
            this.shutdownTimeout=shutdownTimeout;
            return this;
        }

        protected String initPort() {
            if (port==null) {
                return ":8080";
            }
            return port;
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
        protected int initTestTimeout() throws GpUnitException {
            if (testTimeout>0) {
                return testTimeout;
            }
            else if (jobCompletionTimeout>0) {
                return 60+jobCompletionTimeout;
            }
            else if (shutdownTimeout>0) {
                return shutdownTimeout;
            }
            else {
                return 1200;
            }
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
        protected int initJobCompletionTimeout() throws GpUnitException {
            if (jobCompletionTimeout>0) {
                return jobCompletionTimeout;
            }
            else if (testTimeout>0) {
                return testTimeout;
            }
            else if (shutdownTimeout>0) {
                return shutdownTimeout;
            }
            else {
                return 900;
            }
        }

        public BatchProperties build() throws GpUnitException {
            return new BatchProperties(this);
        }

    }

}
