package org.genepattern.gpunit.exec.rest;

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.download.JobResultDownloader;
import org.genepattern.gpunit.exec.rest.json.JobResultObj;
import org.genepattern.gpunit.test.BatchModuleTestObject;
import org.genepattern.gpunit.test.BatchProperties;
import org.genepattern.gpunit.validator.JobResultValidatorGeneric;
import org.junit.Assert;

public class JobResultValidatorRest extends JobResultValidatorGeneric {
    private JobResultObj jobResult;
    private JobRunnerRest restClient;
    private JobResultDownloaderRest downloader=null;

    public JobResultValidatorRest(final BatchProperties props, final BatchModuleTestObject batchTestObject, final File downloadDir) {
        super(props, batchTestObject, downloadDir);
    }
    
    public void setRestClient(JobRunnerRest restClient) {
        this.restClient=restClient;
    }
    
    public void setJobStatus(final JobResultObj jobResult) {
        this.jobResult=jobResult;
    }

    @Override
    public void checkInit() {
        assertNotNull("jobResult==null", jobResult);
        final String jobId=jobResult.getJobId();
        assertNotNull("jobId==null", jobId);
        super.setJobId(jobId);
        
        Assert.assertNotNull("restClient==null", restClient);
        this.downloader=new JobResultDownloaderRest(getDownloadDir(), getBatchProperties());
        try {
            this.downloader.setRestClient(restClient);
            this.downloader.setJobResult(jobResult);
        }
        catch (Throwable t) {
            Assert.fail("Error initializing validator for jobId="+getJobId()+
                    ": "+t.getLocalizedMessage());
        }
    }

    @Override
    public boolean hasStdError() {
        try {
            return jobResult.hasError();
        }
        catch (Exception e) {
            Assert.fail("Error getting 'hasError' for jobId="+
                    getJobId()+": "+e.getLocalizedMessage());
        }
        catch (Throwable t) {
            Assert.fail("Unexpected exception: "+t.getLocalizedMessage());
        }
        //shouldn't be here
        return true;
    }

    @Override
    public JobResultDownloader getDownloader() {
        if (downloader == null) {
            Assert.fail("downloader not initialized");
        }
        return downloader;
    }

    @Override
    public void deleteJob() throws GpUnitException {
        throw new GpUnitException("Method not implemented!");
    }

}
