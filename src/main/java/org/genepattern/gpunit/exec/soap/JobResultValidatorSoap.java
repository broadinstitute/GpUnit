package org.genepattern.gpunit.exec.soap;

import java.io.File;

import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.download.soap.JobResultDownloaderSoap;
import org.genepattern.gpunit.test.BatchModuleTestObject;
import org.genepattern.gpunit.test.BatchProperties;
import org.genepattern.gpunit.validator.JobResultValidatorGeneric;
import org.genepattern.webservice.JobResult;

import org.junit.Assert;

public class JobResultValidatorSoap extends JobResultValidatorGeneric {
    private JobResult jobResult=null;
    private JobResultDownloaderSoap downloader=null;
    private ModuleRunner moduleRunner;

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
    
    public void setModuleRunner(final ModuleRunner moduleRunner) {
        this.moduleRunner=moduleRunner;
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
    /**
     * Read the error message by downloading 'stderr.txt' result file and returning 
     * a String containing the first MAX_N lines of the file.
     * 
     * @return
     */
    public String getErrorMessageFromStderrFile() {
        String errorMessage="";
        File stderrFile = null;
        try {
            stderrFile=downloader.getResultFile("stderr.txt");
        }
        catch (Throwable t) {
            errorMessage = "There was an error downloading 'stderr.txt': "+t.getLocalizedMessage();
        }
        errorMessage = JobResultValidatorOrig.getErrorMessageFromStderrFile(stderrFile);
        return errorMessage;
    }

    @Override
    public void downloadResultFiles(File toDir) throws GpUnitException {
        downloader.downloadResultFiles();
    }

    @Override
    public int getNumResultFiles() {
        return downloader.getNumResultFiles();
    }

    @Override
    public boolean hasResultFile(String relativePath) {
        return downloader.hasResultFile(relativePath);
    }

    @Override
    public File getResultFile(File toDir, String relativePath) throws GpUnitException {
        return downloader.getResultFile(relativePath);
    }

    @Override
    public void cleanDownloadedFiles() throws GpUnitException {
        downloader.cleanDownloadedFiles();
    }

    @Override
    public void deleteJob() throws GpUnitException {
        if (moduleRunner==null) {
            throw new GpUnitException("moduleRunner==null");
        }
        moduleRunner.deleteJob(jobResult.getJobNumber());
    }


}
