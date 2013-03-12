package org.genepattern.gpunit.exec.rest;

import java.io.File;
import java.net.URI;
import java.net.URL;

import org.genepattern.gpunit.GpAssertions;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.gpunit.exec.soap.JobResultValidatorOrig;
import org.genepattern.gpunit.test.BatchModuleTestObject;
import org.genepattern.gpunit.test.BatchProperties;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;

public class RestClientUtil {
    
    public static void runTest(BatchProperties batchProps, BatchModuleTestObject testObj) throws Exception {
        JobRunnerRest runner=new JobRunnerRest(batchProps,testObj.getTestCase());
        //1) run the job
        final URI jobUri=runner.submitJob();

        //2) poll for job completion
        int count=0;
        int maxtries = 20;
        int initialSleep = 1000;
        JSONObject job=waitForJob(runner, jobUri, initialSleep, initialSleep, maxtries, count); 
        //boolean isFinished=jobStatus.getBoolean("isFinished");

        //3) validate job results  
        String jobId=job.getString("jobId");

        JSONObject jobStatus=job.getJSONObject("status");
        boolean hasError=jobStatus.getBoolean("hasError");

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
                errorMessage=JobResultValidatorOrig.getErrorMessageFromStderrFile(stderrFile);
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
    
    //helper methods for polling for job completion, could be made generic
    private static JSONObject waitForJob(JobRunnerRest runner, URI jobUri, int sleep, int initialSleep, int maxTries, int count) 
    throws InterruptedException, GpUnitException
    {
        Thread.sleep(sleep);
        JSONObject job=null;
        try {
            job=runner.getJob(jobUri);
        }
        catch (Exception e) {
            throw new GpUnitException("Error getting jobStatus from: "+jobUri, e);
        }
        if (job==null) {
            throw new IllegalArgumentException("job==null");
        }
        boolean isFinished;
        try {
            isFinished=job.getJSONObject("status").getBoolean("isFinished");
        }
        catch (JSONException e) {
            throw new GpUnitException("Error parsing JSON object from: "+jobUri, e);
        }
        if (isFinished) {
            return job;
        }
        count++;
        sleep = incrementSleep(initialSleep, maxTries, count);
        return waitForJob(runner, jobUri, sleep, initialSleep, maxTries, count);
    }
    
    /**
     * Make the sleep time go up as it takes longer to exec. eg for 100 tries of 1000ms (1 sec) first 20 are 1 sec each
     * next 20 are 2 sec each next 20 are 4 sec each next 20 are 8 sec each any beyond this are 16 sec each
     */
    private static int incrementSleep(int init, int maxTries, int count) {
        if (count < (maxTries * 0.2)) {
            return init;
        }
        if (count < (maxTries * 0.4)) {
            return init * 2;
        }
        if (count < (maxTries * 0.6)) {
            return init * 4;
        }
        if (count < (maxTries * 0.8)) {
            return init * 8;
        }
        return init * 16;
    }

    //helper methods for job result validation
    private static class GenericJobResult {
        final static public String NL = System.getProperty("line.separator");
        public String jobId;
        public boolean hasError;
        //TODO: GET this from the REST call
        public String errorMessage="Error message, see stderr.txt file on GP server for more details";
    }

    private static void validateJobStatus(final ModuleTestObject testCase, final GenericJobResult jobResult) {
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
