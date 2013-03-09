package org.genepattern.gpunit.test;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.genepattern.client.GPClient;
import org.genepattern.gpunit.GpAssertions;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.gpunit.exec.rest.JobRunnerRest;
import org.genepattern.gpunit.yaml.JobResultValidator;
import org.genepattern.gpunit.yaml.ModuleRunner;
import org.genepattern.gpunit.yaml.Util;
import org.genepattern.util.junit.Parallelized;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;


/**
 * This is the parallelized junit test for a batch of gp-unit test cases.
 * 
 * Run one unit test for each gp-unit test file in the given fileset.
 * 
 * See the example build.xml file for details on how to set the fileset.
 * 
 * @author pcarr
 */
@RunWith(Parallelized.class)
public class BatchModuleTest { 
    static GPClient gpClient;
    static BatchProperties batchProps;

    final static private String PROP_TESTCASE_DIRS="gpunit.testcase.dirs";

    /**
     * This parameterized test runs a single unit test for each test case in the Collection.
     * Each Object[] instance in the collection is used to call the BatchModuleTest constructor,
     *     e.g. new BatchModuleTest(Object[0], Object[1], Object[2])
     *     
     * see the build.xml file for an example of how to configure the list of test-cases from ant,
     * for example,
     * <pre>
        <path id="gpunit.testcase.path">
            <pathelement location="./tests/protocols/01_Run" />
            <pathelement location="./tests/saved_jobs" />
            <pathelement location="./tests/protocols/02_Differential/03_CMSViewer/test.yaml">
        </path>
        <property name="gpunit.testcase.dirs" refid="gpunit.testcase.path" /> 
        </pre>
     *
     */
    @Parameters(name="{1}")
    public static Collection<Object[]> data() throws GpUnitException {
        //TODO: set debug=false before checking in, it's here for debugging new gp-unit features
        final boolean debug=false;
        if (debug) {
            initDebug();
        }
        
        String numThreadsProp = System.getProperty("junit.parallel.threads");
        if (numThreadsProp == null) {
            System.setProperty("junit.parallel.threads", "32");
        }
        
        Collection<Object[]> testCases;

        String gpunitTestcaseDirsProp = System.getProperty(PROP_TESTCASE_DIRS);
        if (gpunitTestcaseDirsProp != null) { 
            //parse the list of one or more test cases
            List<File> fileset = new ArrayList<File>();
            String[] testCaseDirs = gpunitTestcaseDirsProp.split(Pattern.quote(File.pathSeparator));
            for(String testCaseDir : testCaseDirs) {
                fileset.add(new File(testCaseDir));
            }
            testCases=BatchModuleUtil.data(fileset);
        }
        else {
            final String path="./tests/protocols"; 
            testCases=BatchModuleUtil.data(new File(path));
        }
        
        
        //validate the testCases, make sure we have a unique download dir for each test
        Set<String> testNames=new HashSet<String>();
        for(Object[] row : testCases) {
            final BatchModuleTestObject batchTestObj=(BatchModuleTestObject) row[2];
            final File testFile=batchTestObj.getTestFile();
            final String testName=Util.getTestNameFromFile(testFile);
            boolean added=testNames.add(testName);
            if (!added) {
                //error, duplicate testName
                throw new GpUnitException("Error duplicate testName, testName="+testName+", testFile="+batchTestObj.getTestFile().getAbsolutePath());
            }
        }
        return testCases;
    }

    private static void initDebug() {
        initRestTest();
        //initDiffTest();
    }

    /**
     * Helper class for initializing properties when launching gp-unit tests from a debugger.
     * For example, when developing new gp-unit features from an Eclipse IDE.
     *  
     * @param gpUrl
     * @param user
     * @param password
     * @param testDir
     */
    private static void _debugInitDefault() {
        //final String gpUrl="http://genepattern.broadinstitute.org";
        //final String gpUrl="http://gpbroad.broadinstitute.org";
        //final String gpUrl="http://genepatternbeta.broadinstitute.org";
        //final String gpUrl="http://gpdev.broadinstitute.org";
        final String gpUrl="http://127.0.0.1:8080";
        
        final String gpUser="test";
        final String gpPass="test";
        
        System.setProperty(BatchProperties.PROP_GP_URL, gpUrl);
        System.setProperty(BatchProperties.PROP_GP_USERNAME, gpUser);
        System.setProperty(BatchProperties.PROP_GP_PASSWORD, gpPass);

        System.setProperty(BatchProperties.PROP_CLIENT, BatchProperties.GpUnitClient.SOAP.toString());
        System.setProperty(BatchProperties.PROP_SAVE_DOWNLOADS, "true");
        System.setProperty(BatchProperties.PROP_DELETE_JOBS, "false");
        //System.setProperty(BatchProperties.PROP_OUTPUT_DIR, "./jobResults"); 
        //System.setProperty(BatchProperties.PROP_BATCH_NAME, "run-"+new Date().getTime()); 
        
        //System.setProperty(PROP_TESTCASE_DIRS, "./tests/protocols");
        System.setProperty(PROP_TESTCASE_DIRS, "./tests/protocols/01_Run");
    }
    
    private static void initRestTest() {
        _debugInitDefault();
        System.setProperty(BatchProperties.PROP_CLIENT, BatchProperties.GpUnitClient.REST.toString());
        System.setProperty(PROP_TESTCASE_DIRS, "./tests/testRestClient");
    }

    private static void initDiffTest() {
        _debugInitDefault();
        System.setProperty(PROP_TESTCASE_DIRS, "./tests/DiffTest");
    }

    @BeforeClass 
    public static void beforeClass() throws GpUnitException {
        gpClient = ModuleRunner.initGpClient();
        batchProps = BatchProperties.Factory.initFromProps();
    }
    
    @AfterClass
    public static void afterClass() throws GpUnitException {
        //if necessary, delete the batch output directory
        //only do this for directories created specifically for this batch run of 
        //    gp-unit tests
        if (!batchProps.getSaveDownloads()) {
            if (batchProps.getCreatedBatchOutputDir()) {
                File batchOutputDir=batchProps.getBatchOutputDir();
                if (batchOutputDir != null) {
                    boolean success=batchOutputDir.delete();
                    if (!success) {
                        String message="failed to delete batch output directory: "+batchOutputDir.getAbsolutePath();
                        throw new GpUnitException(message);
                    }
                }
            }
        }
    }

    private int batchIdx;
    //testname is the way to get the name for the test to show up in the junit report
    private String testname;
    private BatchModuleTestObject testObj;
    
    public BatchModuleTest(final int batchIdx, final String testname, final BatchModuleTestObject testObj) {
        this.batchIdx = batchIdx;
        this.testname = testname;
        this.testObj = testObj;
    }
    
    @Test
    public void runJobAndWait() throws Exception {
        try {
            //submit a job via new REST API
            if (batchProps.getClient().equals(BatchProperties.GpUnitClient.REST)) {
                JobRunnerRest runner=new JobRunnerRest(batchProps,testObj.getTestCase());
                //1) run the job
                final URI jobUri=runner.submitJob();

                //2) poll for job completion
                //TODO: implement polling, as a hack, sleep for a few seconds
                Thread.sleep(2500);
                JSONObject job=runner.getJob(jobUri);
                String jobId=job.getString("jobId");
                
                JSONObject jobStatus=job.getJSONObject("status");
                boolean isFinished=jobStatus.getBoolean("isFinished");
                boolean hasError=jobStatus.getBoolean("hasError");
                
                if (!isFinished) {
                    //TODO: poll for job completion
                }
                
                //3) validate job results  
                //   TODO: merge with GPClient specific functionality in Util.runTest, see JobResultValidator
                //   JobResultValidator is hard-coded to use the GPclient SOAP client, we need to refactor into two 
                //   implementations, one for SOAP client one for REST client
                //testObj.getTestCase().getAssertions().getJobStatus();
                //if (hasError) {
                //    throw new Exception("job hasError");
                //}

                //TODO: need to incorporate full support for download directory
                //    In the Soap client implementation, we automatically create directories as needed,
                //    and automatically delete the ones we created
                File jobResultDir=batchProps.getJobResultDir(testObj, jobId);
                if (!jobResultDir.exists()) {
                    boolean success=jobResultDir.mkdirs();
                    if (!success) {
                        Assert.fail("Failed to create jobResultDir="+jobResultDir);
                    }
                }

                File stderrFile=null;
                String errorMessage=null;
                if (hasError) {
                    //init the error message
                    String stderrLocation=jobStatus.getString("stderrLocation");
                    if (stderrLocation != null) {
                        URL stderrUrl = new URL(stderrLocation);
                        //get the filename from the url
                        //TODO: could include this in the jobStatus object
                        String stderrFilename="stderr.txt";
                        String p=stderrUrl.getPath();
                        if (p != null) {
                            int idx=p.lastIndexOf("/");
                            if (idx>0) {
                                p=p.substring(idx);
                                if (p.startsWith("/")) {
                                    p=p.substring(1);
                                }
                            }
                            if (p.length()==0) {
                                p="stderr.txt";
                            }
                            stderrFilename=p;
                        }
                        stderrFile=new File(jobResultDir, stderrFilename);
                        runner.downloadFile(stderrUrl, stderrFile);
                        errorMessage=JobResultValidator.getErrorMessageFromStderrFile(stderrFile);
                    }
                }
                
                GenericJobResult jobResult=new GenericJobResult();
                jobResult.jobId=job.getString("jobId");
                jobResult.hasError=hasError;
                if (errorMessage != null) {
                    jobResult.errorMessage=errorMessage;
                }
                
                validateJobStatus(testObj.getTestCase(), jobResult);
                return;
            }

            Util.runTest(gpClient,batchProps,testObj);
        }
        catch (Throwable t) {
            Assert.fail(t.getLocalizedMessage());
        }
    }
    
    //helper methods for job result validation
    private static class GenericJobResult {
        final static public String NL = System.getProperty("line.separator");
        public String jobId;
        public boolean hasError;
        //TODO: GET this from the REST call
        public String errorMessage="Error message, see stderr.txt file on GP server for more details";
    }

    private void validateJobStatus(final ModuleTestObject testCase, final GenericJobResult jobResult) {
        GpAssertions assertions = testCase.getAssertions();
        
        boolean expectedHasStdError = false;
        if (assertions != null && assertions.getJobStatus().trim().length() > 0) {
            //check to see if it's a test-case with an expected stderr.txt output
            expectedHasStdError = !"success".equalsIgnoreCase(assertions.getJobStatus());
        }
        
        //case 1: expecting stderr
        if (expectedHasStdError) {
            Assert.assertTrue("job #"+jobResult.jobId+" doesn't have stderr.txt output", jobResult.hasError);
            return;
        }
        //case 2: unexpected stderr
        if (jobResult.hasError && !expectedHasStdError) {
            String junitMessage = "job #"+jobResult.jobId+" has stderr.txt output: ";
            //TODO: lazy init error message
            //    for SOAP gpclient, String errorMessage = getErrorMessageFromStderrFile();
            junitMessage += GenericJobResult.NL + jobResult.errorMessage;
            Assert.fail(junitMessage);
        }
    }


}
