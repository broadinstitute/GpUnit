package org.genepattern.gpunit.soap;

import java.io.File;

import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.download.JobResultDownloader;
import org.genepattern.gpunit.BatchModuleTestObject;
import org.genepattern.gpunit.BatchProperties;
import org.genepattern.gpunit.validator.JobResultValidatorGeneric;
import org.genepattern.webservice.JobResult;

import org.junit.Assert;

public class JobResultValidatorSoap extends JobResultValidatorGeneric {
    private JobResult jobResult=null;
    private JobResultDownloader downloader=null;
    private ModuleRunnerSoap moduleRunnerSoap;

    public JobResultValidatorSoap(final BatchProperties props, final BatchModuleTestObject batchTestObject, final File downloadDir) {
        super(props, batchTestObject, downloadDir);
    }
    
    public void setJobResult(final JobResult jobResult) {
        if (jobResult==null) {
            throw new IllegalArgumentException("jobResult==null");
        }
        this.jobResult=jobResult;
        setJobId(""+jobResult.getJobNumber());
    }
    
    public void setModuleRunner(final ModuleRunnerSoap moduleRunnerSoap) {
        this.moduleRunnerSoap=moduleRunnerSoap;
    }

    @Override
    public void checkInit() {
        //1) null jobResult
        Assert.assertNotNull("jobResult is null", jobResult);
        Assert.assertTrue("jobNumber must be >= 0", jobResult.getJobNumber()>=0);
        
        this.downloader=new JobResultDownloaderSoap(getDownloadDir(), getBatchProperties(), jobResult);
    }

    @Override
    public boolean hasStdError() {
        return jobResult.hasStandardError();
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
        if (moduleRunnerSoap==null) {
            throw new GpUnitException("moduleRunner==null");
        }
        moduleRunnerSoap.deleteJob(jobResult.getJobNumber());
    }

}
