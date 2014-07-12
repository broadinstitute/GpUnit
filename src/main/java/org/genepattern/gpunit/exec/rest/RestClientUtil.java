package org.genepattern.gpunit.exec.rest;

import java.io.File;
import java.net.URI;

import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.test.BatchModuleTestObject;
import org.genepattern.gpunit.test.BatchProperties;
import org.json.JSONException;
import org.json.JSONObject;

public class RestClientUtil {
    
    public static void runTest(BatchProperties batchProps, BatchModuleTestObject testObject) throws Exception {
        JobRunnerRest runner=new JobRunnerRest(batchProps, testObject.getTestCase());
        //1) run the job
        final URI jobUri=runner.submitJob();

        //2) poll for job completion
        int count=0;
        int maxtries = 20;
        int initialSleep = 1000;
        JSONObject jobResult=waitForJob(runner, jobUri, initialSleep, initialSleep, maxtries, count); 

        //3) validate job results  
        String jobId=jobResult.getString("jobId");
        File jobResultDir=batchProps.getJobResultDir(testObject, jobId);
        
        JobResultValidatorRest validator=new JobResultValidatorRest(batchProps, testObject, jobResultDir);
        validator.setRestClient(runner);
        validator.setJobStatus(jobResult);
        try {
            validator.validate();
        }
        finally {
            if (jobResult != null) {
                validator.clean();
            }
        }
    }
    
    //helper methods for polling for job completion, could be made generic
    private static JSONObject waitForJob(final JobRunnerRest runner, final URI jobUri, final int sleep, final int initialSleep, final int maxTries, final int count) 
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
        return waitForJob(runner, jobUri, incrementSleep(initialSleep, maxTries, count), initialSleep, maxTries, count+1);
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
}
